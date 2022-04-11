package nb.domain

import akka.pattern.ask
import akka.util.Timeout
import nb.Publisher
import nb.domain.Operations.{Account, Transaction, TransactionResponse}
import nb.domain.violations.Violation.DoubledTransaction
import nb.helper.TestHelper
import spray.json.enrichAny

import scala.language.postfixOps
import nb.protocol.OperationsProtocol._

class PublisherSpec extends TestHelper{
  var response: String =""
  def outputMock(x: Any) : Unit = {
    response = x.toString
  }

  "Publisher actor" must {

    "convert OperationResponse and publish it in json format" in{
      val res = TransactionResponse(Some(Account(true, 100)), Set(DoubledTransaction))
      val publisher = system.actorOf(Publisher.props(outputMock))
      publisher ! res
      //This is async process, I could have dealt with that by response from the actor and wait for the response.
      // but felt in this case, that it is wrong to change the code to support test.
      Thread.sleep(100)
      response shouldEqual res.toJson.toString()
    }
  }
}
