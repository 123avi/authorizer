package avi

import akka.actor.{Actor, ActorLogging, Props}

object Publisher{
  def props = Props[Publisher]
}

class Publisher extends Actor with ActorLogging{
  override def receive: Receive = {
    case msg =>
      println(msg)
  }
}
