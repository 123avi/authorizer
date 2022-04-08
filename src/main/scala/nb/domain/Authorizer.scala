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
  import context.dispatcher
  import nb.utils.AuthorizerConfig._

  log.info(s"Starting authorizer with max-transactions of $maxTransactionFrequency and interval of $transactionInterval seconds")

  case class ExecuteTransaction(t: Transaction)

  override def receive: Receive = waitForInit

  def waitForInit : Receive = {
    case i:InitOperation if i.account.activeCard=>
      replyTo ! InitOperationResponse(i.account, Set())
      context.become(run(i.account.availableLimit, Set()).orElse(defaultViolation("waitForInit -> run")))

    case i:InitOperation =>
      replyTo ! InitOperationResponse(i.account, Set())
      context.become(inactiveAccount().orElse(defaultViolation("inactive")))

    case transactionOp: TransactionOperation =>
      replyTo ! TransactionResponse(transactionOp.transaction, Set(AccountNotInitialized))
  }

  def run(limit: Double, transactions: Set[Transaction]) : Receive = {

    case TransactionOperation(t) if transactions.exists(trans => trans.amount == t.amount && trans.merchant == t.merchant
      && isWithinInterval(trans.time, t.time)) =>
      replyTo ! TransactionResponse(t.copy(amount = limit), Set(DoubledTransaction))

    case TransactionOperation(t)  if takeLast(transactions + t).length > maxTransactionFrequency =>
      replyTo ! TransactionResponse(t.copy(amount = limit), Set (HighFrequencySmallInterval))

    case TransactionOperation(t) if limit < t.amount =>
      replyTo ! TransactionResponse(t, Set(InsufficientLimit))

    case TransactionOperation(t)  =>
      val newLimit = limit - t.amount
      context.become(run(newLimit, transactions + t))
      replyTo ! TransactionResponse(t.copy(amount = newLimit), Set())

  }

  def inactiveAccount(): Receive = {
    case TransactionOperation(transaction) =>
      replyTo ! TransactionResponse(transaction, Set(CardNotActive))
  }

  def defaultViolation(logMsg:String) : Receive = {
    case i:InitOperation =>
      replyTo ! InitOperationResponse(i.account, Set(AccountAlreadyInitialized))

    case msg =>
      log.warning(s"Unexpected messaged received: $msg with $logMsg")
  }
}
