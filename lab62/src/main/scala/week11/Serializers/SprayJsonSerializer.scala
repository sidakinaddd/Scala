package week11.Serializers

import spray.json.{DefaultJsonProtocol, RootJsonFormat}
import week11.model.Path
import week11.model.{ErrorResponse, SuccessfulResponse}

trait SprayJsonSerializer extends  DefaultJsonProtocol {
  implicit val successfulResponse: RootJsonFormat[SuccessfulResponse] = jsonFormat2(SuccessfulResponse)
  implicit val errorResponse: RootJsonFormat[ErrorResponse] = jsonFormat2(ErrorResponse)
  implicit val pathFormat:RootJsonFormat[Path]=jsonFormat1(Path)
}
