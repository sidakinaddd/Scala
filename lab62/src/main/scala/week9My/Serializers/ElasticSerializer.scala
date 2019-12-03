package week9My.Serializers

import com.sksamuel.elastic4s.{Hit, HitReader, Indexable}
import week9My.model.Book
import spray.json._

import scala.util.Try


trait ElasticSerializer extends SprayJsonSerializer {

  // object -> JSON string
  implicit object BookIndexable extends Indexable[Book] {
    override def json(book: Book): String = book.toJson.compactPrint
  }

  // JSON string -> object
  // parseJson is a Spray method
  implicit object BookHitReader extends HitReader[Book] {
    override def read(hit: Hit): Either[Throwable, Book] = {
      Try {
        val jsonAst = hit.sourceAsString.parseJson
        jsonAst.convertTo[Book]
      }.toEither
    }
  }
}
