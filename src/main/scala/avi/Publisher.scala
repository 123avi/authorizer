package avi

import akka.actor.{Actor, ActorLogging, Props}
import avi.domain.Operations._

object Publisher{
  def props = Props[Publisher]
}

class Publisher extends Actor with ActorLogging{
  import avi.protocol.OperationsProtocol._
  import spray.json._

  override def receive: Receive = {
    case msg:OperationResponse =>
      println(msg.toJson)
  }
}
