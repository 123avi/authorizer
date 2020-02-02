package avi.protocol

import avi.domain.Operations._
import avi.domain.violations.Violation
import spray.json._

object OperationsProtocol extends DefaultJsonProtocol {



  implicit object accountProtocol extends RootJsonFormat[Account] {
    override def read(json: JsValue): Account = {
      json.asJsObject.getFields("active-card", "available-limit") match {
        case Seq(JsBoolean(active), JsNumber(limit)) =>
          Account(active, limit.toDouble)
        case _ => throw  DeserializationException("Account expected")
      }
    }

    override def write(obj: Account): JsValue = JsObject(
      "active-card"     -> JsBoolean (obj.activeCard),
      "available-limit" -> JsNumber(obj.availableLimit)
    )
  }

  //{"transaction": {"merchant": "Burger King", "amount": 20, "time":"2019-02-13T10:00:00.000Z"}}

  implicit val initOperationProtocol = jsonFormat1(InitOperation)
  implicit object ViolationFormat extends RootJsonWriter[Violation] {
    override def write(obj: Violation): JsValue = JsString(obj.asString)
  }

  implicit val transactionFormat = jsonFormat3(Transaction)
  implicit val transactionOperationFormat = jsonFormat1(TransactionOperation)

  implicit object operationFormat extends RootJsonFormat[Operation] {
    override def read(json: JsValue): Operation ={
      if (json.asJsObject.fields.keySet.contains("transaction"))
        json.convertTo[TransactionOperation]
      else
        json.convertTo[InitOperation]
    }

    override def write(obj: Operation): JsValue = obj match {
      case i:InitOperation => i.toJson
      case t:TransactionOperation =>t.toJson
    }
  }

  implicit object InitOperationResponseProtocol extends RootJsonWriter[InitOperationResponse] {
    override def write(obj: InitOperationResponse): JsValue = JsObject(
      "account" -> obj.account.toJson,
      "violations" -> JsArray(obj.violations.toVector.map(_.toJson))
    )
  }

  implicit object TransactionResponseProtocol extends RootJsonWriter[TransactionResponse] {
    override def write(obj: TransactionResponse): JsValue = JsObject(
      "transaction" -> obj.transaction.toJson,
      "violations" -> JsArray(obj.violations.toVector.map(_.toJson))
    )
  }

  implicit object operationResponseFormat extends RootJsonWriter[OperationResponse] {
    override def write(obj: OperationResponse): JsValue = obj match {
      case i:InitOperationResponse => i.toJson
      case t:TransactionResponse =>t.toJson
    }
  }
}
