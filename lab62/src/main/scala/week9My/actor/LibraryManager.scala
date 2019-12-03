package week9My.actor

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, RequestEntity}
import akka.stream.{ActorMaterializer, Materializer}
import com.sksamuel.elastic4s.http.ElasticDsl.{indexInto, _}
import com.sksamuel.elastic4s.http.HttpClient
import com.typesafe.config.{Config, ConfigFactory}
import week9My.Serializers.{ElasticSerializer, Serializer}
import week9My.model.{Book, ErrorResponse, SuccessfulResponse, telegramMessage}

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}


object LibraryManager {
  val index="books"
  val inIndex="_doc"
  case class GetAllBooks()
  case class CreateBook(book: Book)

  case class ReadBook(id:String)

  case class UpdateBook(movie:Book)

  case class DeleteBook(id:String)


  def props(Client:HttpClient)=Props(new LibraryManager(Client))
}
class LibraryManager(Client:HttpClient) extends  Actor with ActorLogging with ElasticSerializer with Serializer{
  import LibraryManager._

  implicit val system: ActorSystem = ActorSystem("telegram-service")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = system.dispatcher
  val config: Config = ConfigFactory.load() // config

  val token = config.getString("telegram.token") // token
  val url=s"https://api.telegram.org/bot730223470:AAEc4zoY4yzJNbKdlnRgaN0gIzMred8TKyM/sendMessage"


  override def receive:Receive={
    case CreateBook(book)=>
      val replyTo=sender()
      val cmd = Client.execute(indexInto(index / inIndex).id(book.id).doc(book))

      cmd.onComplete{
        case Success(value)=>
          log.info("Book with ID :{} created",book.id)
          replyTo ! Right(SuccessfulResponse(201,s"Book with ID: ${book.id} created"))
          val message:telegramMessage=telegramMessage(token, s"Book with ID = ${book.id} successfully created. Book = [$book]")
          val httpReq=Marshal(message).to[RequestEntity].flatMap{entity=>
           val request=HttpRequest(HttpMethods.POST,url,Nil,entity)
           log.debug("Request:{}",request)
           Http().singleRequest(request)
         }

          httpReq.onComplete{
            case Success(value)=>
              log.info(s"Response:$value")
              value.discardEntityBytes()
            case Failure(exception)=>
            log.error("error")
          }



        case Failure(_) =>
          log.warning(s"Could not create a book with ID: ${book.id} because it already exists.")
          replyTo ! Left(ErrorResponse(409, s"Book with ID: ${book.id} already exists."))
      }
//
//    case GetAllBooks()=>
//      books.isEmpty match {
//        case false=>
//          sender()! Right(books)
//        case true=>
//          sender()! Left(ErrorResponse(404,"There are no books"))
//      }

    case msg:ReadBook=>
      val replyTo=sender()
      val cmd=Client.execute{
        get(msg.id).from(index/inIndex)
      }
        cmd.onComplete{

        case Success(either)=>
          either.map(e => e.result.safeTo[Book]).foreach { book => {
            book match {
              case Left(_) =>
                log.info("Book with ID: {} not found [GET].", msg.id);
                replyTo ! Left(ErrorResponse(404, s"Book with ID: ${msg.id} not found [GET]."))
              case Right(book) =>
                log.info("Book with ID: {} found [GET].", msg.id)
                replyTo ! Right(book)

                val message:telegramMessage=telegramMessage(token,s"Book with ID = ${book.id} successfully got. Book = [$book]")

                val httpReq = Marshal(message).to[RequestEntity].flatMap { entity =>
                  val request = HttpRequest(HttpMethods.GET, url, Nil, entity)
                  log.debug("Request: {}", request)
                  Http().singleRequest(request)
                }
                httpReq.onComplete {
                  case Success(value) =>
                    log.info(s"Response: $value")
                    value.discardEntityBytes()

                  case Failure(exception) =>
                    log.error("error")
                }

            }
            }
          }
        case Failure(fail)=>
          log.warning(s"Could not read a book with ID: ${msg.id}. Exception with MESSAGE: ${fail.getMessage} occurred during this request. [GET]")
          replyTo ! Left(ErrorResponse(500, fail.getMessage))
      }

    case UpdateBook(book)=>
      val replyTo=sender()
      val cmd=Client.execute{
        update(book.id).in(index/inIndex).doc(book)
      }
        cmd.onComplete{
        case Success(_)=>
          log.info("Book with ID: {} updated.",book.id)
          replyTo ! Right(SuccessfulResponse(200,s"Book with ID:${book.id} updated"))


        case Failure(_)=>

      }

    case DeleteBook(id)=>
      val replyTo=sender()
      val cmd=Client.execute{
        delete(id).from(index/inIndex)
      }
        cmd.onComplete{
        case Success(either) =>
          either.map(e => e.result.result.toString).foreach { res => {
            res match {
              case "deleted" =>
                log.info("Book with ID: {} deleted.", id);
                replyTo ! Right(SuccessfulResponse(200, s"Book with ID: ${id} deleted."))

              case "not_found" =>
                log.info("Book with ID: {} not found [DELETE].", id);
                replyTo ! Left(ErrorResponse(404, s"Book with ID: ${id} not found [DELETE]."))
            }
          }
          }
        case Failure(fail) =>
          log.warning(s"Could not delete a book with ID: ${id}. Exception with MESSAGE: ${fail.getMessage} occurred during this request. [DELETE]")
          replyTo ! Left(ErrorResponse(500, fail.getMessage))
      }

  }




}