package avi.helper

import akka.actor.ActorSystem
import akka.testkit.{ ImplicitSender, TestActors, TestKit }
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

abstract class TestHelper extends TestKit(ActorSystem("AuthorizerSpecSystem"))
  with ImplicitSender
  with AnyWordSpecLike
  with Matchers
  with BeforeAndAfterAll {

//  implicit val system = ActorSystem("AothorizerSpec")


  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }
}
