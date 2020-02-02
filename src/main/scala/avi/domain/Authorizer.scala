package avi.domain
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import avi.domain.Operations._
import avi.domain.violations.Violation._

import scala.language.postfixOps

object Authorizer{
  def props( replyTo: ActorRef) = Props(new Authorizer(replyTo))
  case object CompleteAggregate

}

class Authorizer( replyTo: ActorRef) extends Actor with ActorLogging {
  import Authorizer._
  import avi.utils.AuthorizerConfig._
  import avi.utils.DateTimeConversions._
  import context.dispatcher

  log.info(s"Starting authorizer with max-transactions of $maxTransaction and interval of $transactionInterval minutes")

  case class ExecuteTransaction(t: Transaction)

  def fireTimer() = context.system.scheduler.scheduleOnce(transactionInterval, self, CompleteAggregate)

  override def receive: Receive = waitForInit

  def waitForInit : Receive = {
    case i:InitOperation if i.account.activeCard=>
      replyTo ! InitOperationResponse(i.account, Set())
      fireTimer()
      context.become(run(i.account.availableLimit, Set()).orElse(defaultViolation("waitForInit -> run")))

    case i:InitOperation =>
      replyTo ! InitOperationResponse(i.account, Set())
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
