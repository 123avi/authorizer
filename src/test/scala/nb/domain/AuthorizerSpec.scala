package nb.domain

import akka.testkit.TestProbe
import nb.domain.Operations._
import nb.domain.violations.Violation
import nb.helper.TestHelper
import nb.utils.AuthorizerConfig

class AuthorizerSpec extends TestHelper {

  "An Authorizer" must {

    "return true between two time strings within the time interval" in {
      val transaction1 = Transaction("Burger King", 50, "2019-02-13T11:00:00.000Z")
      val transaction2 = Transaction("McDonald's", 50, "2019-02-13T11:01:01.000Z")
      val interval = AuthorizerConfig.transactionInterval
      Authorizer.isWithinInterval(transaction1.time, transaction2.time, interval) shouldBe true
    }

    "return false between two time strings exceeding the time interval" in {
      val transaction1 = Transaction("Burger King", 50, "2019-02-13T11:00:00.000Z")
      val transaction2 = Transaction("Burger King", 50, "2019-02-13T10:03:30.000Z")

      val interval = AuthorizerConfig.transactionInterval
      Authorizer.isWithinInterval(transaction1.time, transaction2.time, interval) shouldBe false
    }

    "return last transactions within number of seconds " in {
      val transaction1 = Transaction("a", 10, "2019-02-13T11:00:00.000Z")
      val transaction2 = Transaction("b", 20, "2019-02-13T11:01:00.000Z")
      val transaction3 = Transaction("d", 30, "2019-02-13T11:02:00.000Z")
      val transaction4 = Transaction("e", 40, "2019-02-13T11:02:10.000Z")
      val transaction5 = Transaction("f", 40, "2019-02-13T11:02:20.000Z")
      val transactions = Set(transaction1, transaction2, transaction3, transaction4, transaction5)

      Authorizer.takeLast(transactions, AuthorizerConfig.transactionInterval) should contain theSameElementsAs
        List(transaction5, transaction4, transaction3, transaction2)
    }

    "Authorizer.takeLast return empty list if no transactions " in {
      val transactions: Set[Transaction] = Set()
      Authorizer.takeLast(transactions, AuthorizerConfig.transactionInterval) shouldEqual Nil
    }


    "Init an account should response without violations" in {
      val probe = TestProbe()
      val activeAccount = Account(true, 500)
      val authorizer = system.actorOf(Authorizer.props( probe.ref))
      authorizer ! InitOperation(activeAccount)
      probe.expectMsg(InitOperationResponse(activeAccount, Set()))
    }

    "respond Account-Already-Initialized rule:Init an active account only once" in {
      val probe = TestProbe()
      val activeAccount = Account(true, 500)

      val authorizer = system.actorOf(Authorizer.props( probe.ref))
      authorizer ! InitOperation(activeAccount)
      probe.expectMsg(InitOperationResponse(activeAccount, Set()))
      authorizer ! InitOperation(activeAccount)
      probe.expectMsg(InitOperationResponse(activeAccount, Set(Violation.AccountAlreadyInitialized)))

    }

    "respond with Account-Already-Initialized rule: Init an inactive account only once" in {
      val probe = TestProbe()
      val account = Account(false, 500)

      val authorizer = system.actorOf(Authorizer.props( probe.ref))
      authorizer ! InitOperation(account)
      probe.expectMsg(InitOperationResponse(account, Set()))
      authorizer ! InitOperation(account)
      probe.expectMsg(InitOperationResponse(account, Set(Violation.AccountAlreadyInitialized)))

    }

    "respond with Account-Already-Initialized rule: Init an inactive account only once after transaction" in {
      val probe = TestProbe()
      val account = Account(true, 500)
      val transaction1 = Transaction("Burger King", 50, "2019-02-13T11:00:00.000Z")


      val authorizer = system.actorOf(Authorizer.props( probe.ref))
      authorizer ! InitOperation(account)
      probe.expectMsg(InitOperationResponse(account, Set()))
      authorizer ! TransactionOperation(transaction1)
      probe.expectMsg(TransactionResponse(Some(account.copy(availableLimit = 450)), Set()))

      authorizer ! InitOperation(account)
      probe.expectMsg(InitOperationResponse(account, Set(Violation.AccountAlreadyInitialized)))

    }



    "respond with AccountNotInitialized rule: No transaction should be accepted when the account is not initialized" in {

      val probe = TestProbe()
      val transaction1 = Transaction("Burger King", 50, "2019-02-13T11:00:00.000Z")
      val authorizer = system.actorOf(Authorizer.props( probe.ref))
      authorizer ! TransactionOperation(transaction1)
      probe.expectMsg(TransactionResponse(None, Set(Violation.AccountNotInitialized)))
    }

    "respond with Card Not Active rule: No transaction should be accepted when the card is not active" in {

      val probe = TestProbe()
      val inactiveAccount = Account(false, 500)
      val transaction1 = Transaction("Burger King", 50, "2019-02-13T11:00:00.000Z")
      val authorizer = system.actorOf(Authorizer.props( probe.ref))
      authorizer ! InitOperation(inactiveAccount)
      probe.expectMsg(InitOperationResponse(inactiveAccount, Set()))
      authorizer ! TransactionOperation(transaction1)
      probe.expectMsg(TransactionResponse(Some(inactiveAccount), Set(Violation.CardNotActive)))

    }

    "response with Insufficient-Limit when exceed the available limit" in {
      val probe = TestProbe()

      val account = Account(true, 1000)
      val transaction1 = Transaction("Vivara", 1250, "2019-02-13T11:00:00.000Z")
      val transaction2 = Transaction("Samsung", 2500, "2019-02-13T11:00:01.000Z")
      val transaction3 = Transaction("Nike", 800, "2019-02-13T11:00:02.000Z")

      val authorizer = system.actorOf(Authorizer.props( probe.ref))
      authorizer ! InitOperation(account)
      probe.expectMsg(InitOperationResponse(account, Set()))
      authorizer ! TransactionOperation(transaction1)
      authorizer ! TransactionOperation(transaction2)
      authorizer ! TransactionOperation(transaction3)

      probe.expectMsg(TransactionResponse(Some(account), Set(Violation.InsufficientLimit)))
      probe.expectMsg(TransactionResponse(Some(account), Set(Violation.InsufficientLimit)))
      probe.expectMsg(TransactionResponse(Some(account.copy(availableLimit = 200)), Set()))

    }

    "response with High-Frequency-Small-Interval when more than 3 transactions on a 2-minute interval" in {
      val probe = TestProbe()

      val account = Account(true, 100)
      val transaction1 = Transaction("Burger King", 20, "2019-02-13T11:00:00.000Z")
      val transaction2 = Transaction("Habbib",      20, "2019-02-13T11:00:01.000Z")
      val transaction3 = Transaction("McDonald's",  20, "2019-02-13T11:01:01.000Z")
      val transaction4 = Transaction("Subway",      20, "2019-02-13T11:01:31.000Z")
      val transaction5 = Transaction("Burger King", 10, "2019-02-13T12:00:31.000Z")

      val authorizer = system.actorOf(Authorizer.props( probe.ref))
      authorizer ! InitOperation(account)
      probe.expectMsg(InitOperationResponse(account, Set()))
      authorizer ! TransactionOperation(transaction1)
      authorizer ! TransactionOperation(transaction2)
      authorizer ! TransactionOperation(transaction3)
      authorizer ! TransactionOperation(transaction4)
      authorizer ! TransactionOperation(transaction5)
      probe.expectMsg(TransactionResponse(Some(account.copy(availableLimit = 80)), Set()))
      probe.expectMsg(TransactionResponse(Some(account.copy(availableLimit = 60)), Set()))
      probe.expectMsg(TransactionResponse(Some(account.copy(availableLimit = 40)), Set()))
      probe.expectMsg(TransactionResponse(Some(account.copy(availableLimit = 40)), Set(Violation.HighFrequencySmallInterval)))
      probe.expectMsg(TransactionResponse(Some(account.copy(availableLimit = 30)), Set()))

    }

    "response with doubled-transaction when more than 1 similar transactions" in {
      val probe = TestProbe()

      val account = Account(true, 100)
      val transaction1 = Transaction("Burger King", 20, "2019-02-13T11:00:00.000Z")
      val transaction2 = Transaction("McDonald's", 10, "2019-02-13T11:00:01.000Z")
      val transaction3 = Transaction("Burger King", 20, "2019-02-13T11:00:02.000Z")
      val transaction4 = Transaction("Burger King", 15, "2019-02-13T11:00:03.000Z")

      val authorizer = system.actorOf(Authorizer.props( probe.ref))
      authorizer ! InitOperation(account)
      probe.expectMsg(InitOperationResponse(account, Set()))
      authorizer ! TransactionOperation(transaction1)
      authorizer ! TransactionOperation(transaction2)
      authorizer ! TransactionOperation(transaction3)
      authorizer ! TransactionOperation(transaction4)
      probe.expectMsg(TransactionResponse(Some(account.copy(availableLimit = 80)), Set()))
      probe.expectMsg(TransactionResponse(Some(account.copy(availableLimit = 70)), Set()))
      probe.expectMsg(TransactionResponse(Some(account.copy(availableLimit = 70)), Set(Violation.DoubledTransaction)))
      probe.expectMsg(TransactionResponse(Some(account.copy(availableLimit = 55)), Set()))

    }

    "response with no violations if there is sufficient limit" in {
      val probe = TestProbe()

      val account = Account(true, 500)
      val transaction1 = Transaction("Burger King", 50, "2019-02-13T11:00:00.000Z")
      val transaction2 = Transaction("Habbib", 50, "2019-02-13T11:00:01.000Z")
      val transaction3 = Transaction("McDonald's", 50, "2019-02-13T11:01:01.000Z")
      val authorizer = system.actorOf(Authorizer.props( probe.ref))
      authorizer ! InitOperation(account)
      probe.expectMsg(InitOperationResponse(account, Set()))
      authorizer ! TransactionOperation(transaction1)
      authorizer ! TransactionOperation(transaction2)
      authorizer ! TransactionOperation(transaction3)
      //      authorizer ! Authorizer.CompleteAggregate
      probe.expectMsg(TransactionResponse(Some(account.copy(availableLimit = 450)), Set()))
      probe.expectMsg(TransactionResponse(Some(account.copy(availableLimit = 400)), Set()))
      probe.expectMsg(TransactionResponse(Some(account.copy(availableLimit = 350)), Set()))
    }
  }

}
