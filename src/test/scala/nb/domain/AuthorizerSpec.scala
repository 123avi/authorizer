package nb.domain

import akka.testkit.TestProbe
import nb.domain.Operations._
import nb.domain.violations.Violation
import nb.helper.TestHelper

class AuthorizerSpec extends TestHelper {
  val activeAccount = Account(true, 500)
  val inactiveAccount = Account(false, 500)
  val transaction1 = Transaction("nb", 50, "2019-02-13T10:00:00.000Z")
  val transaction2 = Transaction("nb", 50, "2019-02-13T10:01:00.000Z")
  val transaction3 = Transaction("nb", 50, "2019-02-13T10:02:00.000Z")
  val transaction4 = Transaction("nb", 50, "2019-02-13T10:01:30.000Z")

  val probe = TestProbe()

  "An Authorizer" must {

    "Init an account should response without violations" in {
      val authorizer = system.actorOf(Authorizer.props( probe.ref))
      authorizer ! InitOperation(activeAccount)
      probe.expectMsg(InitOperationResponse(activeAccount, Set()))
    }

    "respond Account-Already-Initialized rule:Init an active account only once" in {

      val authorizer = system.actorOf(Authorizer.props( probe.ref))
      authorizer ! InitOperation(activeAccount)
      probe.expectMsg(InitOperationResponse(activeAccount, Set()))
      authorizer ! InitOperation(activeAccount)
      probe.expectMsg(InitOperationResponse(activeAccount, Set(Violation.AccountAlreadyInitialized)))

    }

    "respond with Account-Already-Initialized rule: Init an inactive account only once" in {

      val authorizer = system.actorOf(Authorizer.props( probe.ref))
      authorizer ! InitOperation(inactiveAccount)
      probe.expectMsg(InitOperationResponse(inactiveAccount, Set()))
      authorizer ! InitOperation(inactiveAccount)
      probe.expectMsg(InitOperationResponse(inactiveAccount, Set(Violation.AccountAlreadyInitialized)))

    }

    "respond with Card Not Active rule: No transaction should be accepted when the card is not active" in {
      val authorizer = system.actorOf(Authorizer.props( probe.ref))
      authorizer ! InitOperation(inactiveAccount)
      probe.expectMsg(InitOperationResponse(inactiveAccount, Set()))
      authorizer ! TransactionOperation(transaction1)
      probe.expectMsg(TransactionResponse(transaction1, Set(Violation.CardNotActive)))

    }

    "response with Insufficient-Limit when exceed the available limit" in {
      val account = activeAccount.copy(availableLimit = 100)
      val authorizer = system.actorOf(Authorizer.props( probe.ref))
      authorizer ! InitOperation(account)
      probe.expectMsg(InitOperationResponse(account, Set()))
      authorizer ! TransactionOperation(transaction1)
      authorizer ! TransactionOperation(transaction2)
      authorizer ! TransactionOperation(transaction3)
      authorizer ! Authorizer.CompleteAggregate
      probe.expectMsg(TransactionResponse(transaction1.copy(amount = 50), Set()))
      probe.expectMsg(TransactionResponse(transaction2.copy(amount = 0), Set()))
      probe.expectMsg(TransactionResponse(transaction3, Set(Violation.InsufficientLimit)))

    }

    "response with High-Frequency-Small-Interval when more than 3 transactions on a 2-minute interval" in {
      val authorizer = system.actorOf(Authorizer.props( probe.ref))
      authorizer ! InitOperation(activeAccount)
      probe.expectMsg(InitOperationResponse(activeAccount, Set()))
      authorizer ! TransactionOperation(transaction1)
      authorizer ! TransactionOperation(transaction2)
      authorizer ! TransactionOperation(transaction3)
      authorizer ! TransactionOperation(transaction4)
      probe.expectMsg(TransactionResponse(transaction4, Set(Violation.HighFrequencySmallInterval)))

    }

    "response with doubled-transaction when more than 1 similar transactions" in {
      val authorizer = system.actorOf(Authorizer.props( probe.ref))
      authorizer ! InitOperation(activeAccount)
      probe.expectMsg(InitOperationResponse(activeAccount, Set()))
      authorizer ! TransactionOperation(transaction1)
      authorizer ! TransactionOperation(transaction2)
      authorizer ! TransactionOperation(transaction3)
      authorizer ! TransactionOperation(transaction2)
      probe.expectMsg(TransactionResponse(transaction2, Set(Violation.DoubledTransaction)))

    }

    "response with no violations if there is sufficient limit" in {
      val authorizer = system.actorOf(Authorizer.props( probe.ref))
      authorizer ! InitOperation(activeAccount)
      probe.expectMsg(InitOperationResponse(activeAccount, Set()))
      authorizer ! TransactionOperation(transaction1)
      authorizer ! TransactionOperation(transaction2)
      authorizer ! TransactionOperation(transaction3)
      authorizer ! Authorizer.CompleteAggregate
      probe.expectMsg(TransactionResponse(transaction1.copy(amount = 450), Set()))
      probe.expectMsg(TransactionResponse(transaction2.copy(amount = 400), Set()))
      probe.expectMsg(TransactionResponse(transaction3.copy(amount = 350), Set()))
    }
  }

}
