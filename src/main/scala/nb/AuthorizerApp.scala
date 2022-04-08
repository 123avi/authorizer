package nb

import akka.actor.ActorSystem
import akka.pattern.gracefulStop
import akka.util.Timeout
import nb.domain.Authorizer
import nb.domain.Operations.Operation
import spray.json._

import scala.concurrent.Await
import scala.language.postfixOps

object AuthorizerApp extends App{
  import protocol.OperationsProtocol._

  import scala.concurrent.duration._
  implicit val timeout = Timeout(5 seconds)

  val system = ActorSystem("AuthorizerApp")

  val publisher = system.actorOf(Publisher.props)
  val authorizer = system.actorOf(Authorizer.props(publisher))
  run()


  def run(): Unit ={
    val input = scala.io.StdIn.readLine()
    if (input.equalsIgnoreCase("stop")) {
     shutdown()
    } else {
      val cmd = input.parseJson.convertTo[Operation]
      authorizer ! cmd
      run()

    }
  }

  def shutdown(): Unit ={
    val terminationTimeout = 5 seconds

    Await.result(gracefulStop(publisher,  terminationTimeout),  terminationTimeout)
    Await.result(gracefulStop(authorizer,  terminationTimeout),  terminationTimeout)

    system.terminate()
    Await.result(system.whenTerminated, terminationTimeout)
  }
}
