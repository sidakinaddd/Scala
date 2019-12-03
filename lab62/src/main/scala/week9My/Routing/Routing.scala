package week9My.Routing

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.sksamuel.elastic4s.http.HttpClient
import week9My.Serializers.SprayJsonSerializer
import week9My.actor.LibraryManager
import week9My.model.{Book, ErrorResponse, SuccessfulResponse}

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._


case class Routing(implicit val system:ActorSystem,implicit val client:HttpClient) extends SprayJsonSerializer{
  implicit val ec: ExecutionContextExecutor=system.dispatcher
  implicit val timeout:Timeout=Timeout(10.seconds)

  val bookService=system.actorOf(LibraryManager.props(client),"book-service")

  val route: Route =
    (path("healthcheck") & get){
      complete{
        "OK"
      }
    } ~
      pathPrefix("book-service"){
         path("book"/Segment){bookId=>
          get{
           complete{
             (bookService ? LibraryManager.ReadBook(bookId)).mapTo[Either[ErrorResponse,Book]]
        }
      }~
      delete{
        complete{
        (bookService ? LibraryManager.DeleteBook(bookId)).mapTo[Either[ErrorResponse,SuccessfulResponse]]
        }
      }
    }~
      (path("book")){
        post{
          entity(as[Book]){book=>
            complete{
              (bookService ? LibraryManager.CreateBook(book)).mapTo[Either[ErrorResponse,SuccessfulResponse]]
            }

          }
        }~
        put{
          entity(as[Book]){book=>
            complete{
              (bookService ? LibraryManager.UpdateBook(book)).mapTo[Either[ErrorResponse,SuccessfulResponse]]
            }
          }
        }
      }
    }

}
