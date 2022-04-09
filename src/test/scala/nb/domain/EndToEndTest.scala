package nb.domain

import nb.Publisher
import nb.domain.Operations.{Account, InitOperation, InitOperationResponse}
import nb.helper.TestHelper
import spray.json.enrichAny

import scala.language.postfixOps
import nb.protocol.OperationsProtocol._


class EndToEndTest extends TestHelper{
  "Authorizer" must{
    var response: String =""
    def outputMock(x: Any) : Unit = {
      response = x.toString
    }

    "Should accept operation and emit the result to the output " in {
      val publisher = system.actorOf(Publisher.props(outputMock))
      val authorizer = system.actorOf(Authorizer.props( publisher))
      val account = Account(true, 1000)

      authorizer ! InitOperation(account)
      Thread.sleep(100)
      response shouldEqual InitOperationResponse(account, Set()).toJson.toString()
    }
  }
}
