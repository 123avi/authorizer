package nb

import akka.actor.{Actor, ActorLogging, Props}
import nb.domain.Operations.OperationResponse

object Publisher{
  def props = Props[Publisher]
}

class Publisher extends Actor with ActorLogging{
  import nb.protocol.OperationsProtocol._
  import spray.json._

  override def receive: Receive = {
    case msg:OperationResponse =>
      println(msg.toJson)
  }
}
