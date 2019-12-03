package week12

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import org.slf4j.LoggerFactory
import week11.model.{ErrorResponse, PhotoResponse}
import week12.actor.PhotoActor
import week12.actor.PhotoActor.GetPhoto

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.concurrent.duration._

object Boot extends App with SprayJsonSerializer {
  implicit  val system:ActorSystem=ActorSystem("photo-service")
  implicit val materializer:ActorMaterializer=ActorMaterializer()

  implicit val timeout: Timeout = Timeout(10.seconds)
  val log=LoggerFactory.getLogger("Boot")
  val clientRegion: Regions = Regions.EU_CENTRAL_1
  val credentials = new BasicAWSCredentials("AKIASNP4R5FU4S36N2YL", "gg84MiVIo0c9su9HVIEK0llTZ9+67ze2uWLGxA6A")
  val client: AmazonS3 = AmazonS3ClientBuilder.standard()
    .withCredentials(new AWSStaticCredentialsProvider(credentials))
    .withRegion(clientRegion)
    .build()
  val bucketName = "lab11-dana-sidakina"
  val a3session = system.actorOf(PhotoActor.props(client, bucketName))
  createBucket(client, bucketName)
  val route=
    path("health"){
      get{
        complete{
          "OK"
        }
      }
    }~
    pathPrefix("photos"){
      path(Segment){photoName=>
        concat(
          get {
            handle((a3session ? GetPhoto(photoName)).mapTo[Either[ErrorResponse, PhotoResponse]])
          }
        )
      }
    }

  val bindingFuture = Http().bindAndHandle(route, "0.0.0.0", 8080)

  def createBucket(s3client: AmazonS3, bucketName: String): Unit = {
    if (!s3client.doesBucketExistV2(bucketName)) {
      s3client.createBucket(bucketName)
      log.info(s"Bucket with name  $bucketName created")
    } else {
      log.info(s"Bucket $bucketName already exists")
      s3client.listBuckets().asScala.foreach(b => log.info(s"Bucket ${b.getName}"))
    }
  }

  def handle(output: Future[Either[ErrorResponse, PhotoResponse]]) = {
    onSuccess(output) {
      case Left(error) => {
        complete(error.status,error.message)
      }
      case Right(photo) => {
        complete(HttpResponse(photo.status, Nil, entity = HttpEntity(ContentType(MediaTypes.`image/png`), photo.message)));
      }
    }
  }
}
