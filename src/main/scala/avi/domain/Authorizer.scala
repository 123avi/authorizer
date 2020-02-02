package avi.domain
import scala.language.postfixOps
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import avi.domain.Operations._
import avi.domain.violations.Violation._
import avi.utils.ConfigUtils

object Authorizer{
  def props( replyTo: ActorRef) = Props(new Authorizer(replyTo))
  case object CompleteAggregate

}

class Authorizer( replyTo: ActorRef) extends Actor with ActorLogging with ConfigUtils{
  import Authorizer._
  import avi.utils.DateTimeConversions._
  import context.dispatcher

  val maxTransaction  = getInt("max-transactions", 3)
  val transactionInterval = getMinutes("transactions-interval-minutes", 2)

  case class ExecuteTransaction(t: Transaction)

  def fireTimer() = context.system.scheduler.scheduleOnce(transactionInterval, self, CompleteAggregate)

  override def receive: Receive = waitForInit

  def waitForInit : Receive = {
    case i:InitOperation if i.account.activeCard=>
      replyTo ! InitOperationResponse(i.account, Set())
      fireTimer()
      log.debug(s"Set to run with limit ${i.account.availableLimit}")
      context.become(run(i.account.availableLimit, Set()).orElse(defaultViolation("waitForInit -> run")))

    case i:InitOperation =>
      replyTo ! InitOperationResponse(i.account, Set())
      log.debug(s"Set to INACTIVE")
      context.become(inactiveAccount().orElse(defaultViolation("inactive")))

    case transactionOp: TransactionOperation =>
      replyTo ! TransactionResponse(transactionOp.transaction, Set(AccountNotInitialized))
  }

  def run(limit: Double, transactions: Set[Transaction]) : Receive = {

    case TransactionOperation(t)  if transactions.contains(t) =>
      replyTo ! TransactionResponse(t, Set(DoubledTransaction))

    case TransactionOperation(t)  if transactions.size >= maxTransaction =>
      replyTo ! TransactionResponse(t, Set (HighFrequencySmallInterval))

    case TransactionOperation(t) if limit < t.amount =>
      replyTo ! TransactionResponse(t, Set(InsufficientLimit))

    case TransactionOperation(t)  =>
//            val availableLimit = limit - t.amount
//            replyTo ! TransactionResponse(t.copy(amount = availableLimit), Set ())
      context.become(run(limit, transactions + t).orElse(defaultViolation("run-TransactionOperation")))

    case CompleteAggregate =>
      context.become(run(limit, Set()).orElse(defaultViolation("run-CompleteAggregate")))
      val ordered = transactions.toList.sortWith(_.time.tryParse.get < _.time.tryParse.get)
      ordered.foreach(t => self ! ExecuteTransaction(t))

      fireTimer()

    case ExecuteTransaction(t) if t.amount > limit =>
      replyTo ! TransactionResponse(t, Set(InsufficientLimit))

    case ExecuteTransaction(t) =>
      val availableLimit = limit - t.amount
      replyTo ! TransactionResponse(t.copy(amount = availableLimit), Set ())
      context.become(run(availableLimit, transactions).orElse(defaultViolation("run-ExecuteTransaction")))

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
