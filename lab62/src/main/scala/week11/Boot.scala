package week11

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import org.slf4j.LoggerFactory
import week11.Serializers.SprayJsonSerializer
import week11.actor.FilesManager
import week11.actor.FilesManager.{DownloadFiles, GetFile, UploadFile, UploadFiles}
import week11.model.{ErrorResponse, Path, SuccessfulResponse}

import scala.concurrent.Future
import scala.concurrent.duration._
object Boot extends App with SprayJsonSerializer {
  implicit val system: ActorSystem = ActorSystem("file-manager-system")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  implicit val timeout: Timeout = Timeout(10.seconds)

  val log = LoggerFactory.getLogger(this.getClass)

  val clientRegion: Regions = Regions.EU_CENTRAL_1

  val credentials = new BasicAWSCredentials("AKIASNP4R5FU4S36N2YL", "gg84MiVIo0c9su9HVIEK0llTZ9+67ze2uWLGxA6A")

  val client: AmazonS3 = AmazonS3ClientBuilder.standard()
    .withCredentials(new AWSStaticCredentialsProvider(credentials))
    .withRegion(clientRegion)
    .build()

  val bucketName = "lab11-dana-sidakina"

  val a3session = system.actorOf(FilesManager.props(client, bucketName))

  createBucket(client, bucketName)

  val route =
    concat(
      path("file"){
        concat(
          get {
            parameters('filename.as[String]){ fileName =>
              handle((a3session ? GetFile(fileName)).mapTo[Either[ErrorResponse, SuccessfulResponse]])
            }
          },
          post {
            entity(as[Path]) { path =>
              handle((a3session ? UploadFile(path.path)).mapTo[Either[ErrorResponse, SuccessfulResponse]])
            }
          }
        )
      },
      pathPrefix("task2") {
        concat(
          path("in") {
            get {
              complete {
                a3session ! DownloadFiles
                "successfully downloaded"
              }
            }
          },
          path("out") {
            get {
              complete {
                a3session ! UploadFiles
                "successfully uploaded"
              }
            }
          }
        )
      }
    )


  val bindingFuture = Http().bindAndHandle(route, "0.0.0.0", 8080)


  def createBucket(client: AmazonS3, bucket: String): Unit = {
    if (!client.doesBucketExistV2(bucket)) {
      client.createBucket(bucket)
      log.info(s"Bucket with name: $bucket created")
    } else {
      log.info(s"Bucket $bucket already exists")
    }
  }

  def handle(output: Future[Either[ErrorResponse, SuccessfulResponse]]) = {
    onSuccess(output) {
      case Left(error) => {
        complete("error")
      }
      case Right(successful) => {
        complete("Successful")
      }
    }
  }
}

