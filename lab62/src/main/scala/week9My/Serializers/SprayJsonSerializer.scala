package week9My.Serializers

import spray.json.{DefaultJsonProtocol, RootJsonFormat}
import week9My.model.{Author, Book, ErrorResponse, SuccessfulResponse}

trait SprayJsonSerializer extends  DefaultJsonProtocol {
  implicit val authorFormat: RootJsonFormat[Author] = jsonFormat5(Author)
  implicit val bookFormat: RootJsonFormat[Book] = jsonFormat6(Book)
  implicit val successfulResponse: RootJsonFormat[SuccessfulResponse] = jsonFormat2(SuccessfulResponse)
  implicit val errorResponse: RootJsonFormat[ErrorResponse] = jsonFormat2(ErrorResponse)
}
