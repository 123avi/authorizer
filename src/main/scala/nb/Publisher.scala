package nb

import akka.actor.{Actor, ActorLogging, Props}
import nb.domain.Operations.OperationResponse

object Publisher{
  def props( output: Any => Unit = println) = Props(new Publisher(output))
}

class Publisher( output: Any => Unit) extends Actor with ActorLogging{
  import nb.protocol.OperationsProtocol._
  import spray.json._

  override def receive: Receive = {
    case msg:OperationResponse =>
      output(msg.toJson)
  }
}
