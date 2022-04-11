package nb.domain
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import nb.domain.Operations._
import nb.domain.violations.Violation._
import nb.utils.{AuthorizerConfig, DateTimeConversions}

import scala.language.postfixOps

object Authorizer{
  import nb.utils.DateTimeConversions._

  def props( replyTo: ActorRef) = Props(new Authorizer(replyTo))
  def isWithinInterval(t1 : String, t2: String, interval: Long = AuthorizerConfig.transactionInterval) : Boolean = {
    t1.tryParse.flatMap(p1 => t2.tryParse.map {p2 =>
      (p1 - p2).abs <= interval}).get
  }

  def takeLast(transactions: Set[Transaction], timeLimitInSeconds: Long = AuthorizerConfig.transactionInterval): List[Transaction] = {
    if (transactions.isEmpty) {
      Nil
    }
    else{
      val ordered = transactions.toList.sortWith(_.time.tryParse.get > _.time.tryParse.get)
      val t = ordered.head.time.tryParse.get
      ordered.takeWhile(trns =>(trns.time.tryParse.get - t).abs <= timeLimitInSeconds )
    }
  }

}

class Authorizer( replyTo: ActorRef) extends Actor with ActorLogging with DateTimeConversions{
  import Authorizer._
  import nb.utils.AuthorizerConfig._

  log.info(s"Starting authorizer with max-transactions of $maxTransactionFrequency and interval of $transactionInterval seconds")

  override def receive: Receive = waitForInit

  def waitForInit : Receive = {
    case i:InitOperation if i.account.activeCard=>
      replyTo ! InitOperationResponse(i.account, Set())
      context.become(run(i.account, Set()).orElse(defaultViolation("waitForInit -> run")))

    case i:InitOperation =>
      replyTo ! InitOperationResponse(i.account, Set())
      context.become(inactiveAccount(i.account).orElse(defaultViolation("inactive")))

    case _: TransactionOperation =>
      replyTo ! TransactionResponse(None, Set(AccountNotInitialized))
  }

  def run(account: Account, transactions: Set[Transaction]) : Receive = {

    case TransactionOperation(t) if transactions.exists(trans => trans.amount == t.amount && trans.merchant == t.merchant
      && isWithinInterval(trans.time, t.time)) =>
      replyTo ! TransactionResponse(Some(account), Set(DoubledTransaction))

    case TransactionOperation(t)  if takeLast(transactions + t).length > maxTransactionFrequency =>
      replyTo ! TransactionResponse(Some(account), Set (HighFrequencySmallInterval))

    case TransactionOperation(t) if account.availableLimit < t.amount =>
      replyTo ! TransactionResponse(Some(account), Set(InsufficientLimit))

    case TransactionOperation(t)  =>
      val newLimit = account.availableLimit - t.amount
      val acc = account.copy(availableLimit = newLimit)
      context.become(run(acc, transactions + t))
      replyTo ! TransactionResponse(Some(acc), Set())

  }

  def inactiveAccount(account: Account): Receive = {
    case TransactionOperation(transaction) =>
      replyTo ! TransactionResponse(Some(account), Set(CardNotActive))
  }

  def defaultViolation(logMsg:String) : Receive = {
    case i:InitOperation =>
      replyTo ! InitOperationResponse(i.account, Set(AccountAlreadyInitialized))

    case msg =>
      log.warning(s"Unexpected messaged received: $msg with $logMsg")
  }
}
