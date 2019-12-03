package week9My

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.{ActorMaterializer, Materializer}
import week9My.Serializers.ElasticSerializer


object Boot extends App with ElasticSerializer {
  implicit val system: ActorSystem=ActorSystem("book-system")
  implicit val materializer:Materializer=ActorMaterializer()
  implicit  val client=ElasticSearchClient.client
//  ElasticSearchClient.createEsIndex("books");

  val route=Routing.Routing().route
  val bindingFuture=Http().bindAndHandle(route,"0.0.0.0",8080)
}