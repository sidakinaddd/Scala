package week12.actor

import akka.actor.{Actor, ActorLogging, Props}
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.GetObjectRequest
import week11.model.{ErrorResponse, PhotoResponse}

object PhotoActor {
  val path="photos"
  case class GetPhoto(fileName:String)

  def props(client:AmazonS3,bucketName:String)= Props(new PhotoActor(client,bucketName))
}

class PhotoActor(client:AmazonS3,bucketName:String) extends Actor with ActorLogging{
  import PhotoActor._
  override def receive:Receive ={
    case GetPhoto(fileName)=>
      val key=fileName
      println("ok")
      if (client.doesObjectExist(bucketName, key)) {
        val fullObject = client.getObject(new GetObjectRequest(bucketName, key)).getObjectContent
        val photoInBytes: Array[Byte] = Stream.continually(fullObject.read).takeWhile(_ != -1).map(_.toByte).toArray

        log.info(s" Successfully found photo with name ${fileName}")
        sender() ! Right(PhotoResponse(200, photoInBytes))
      } else {
        log.info(s" Failed to get photo with name ${fileName}. It doesn't exist")
        sender() ! Left(ErrorResponse(404, s"Failed to get photo with name ${fileName}. It doesn't exist"))
      }

  }
}
