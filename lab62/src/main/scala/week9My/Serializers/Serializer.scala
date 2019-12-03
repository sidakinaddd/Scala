package week9My.Serializers

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, RootJsonFormat}
import week9My.model.telegramMessage


trait Serializer extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val messageFormat:RootJsonFormat[telegramMessage]=jsonFormat2(telegramMessage)
}
