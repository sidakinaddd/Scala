package week11.actor

import java.io.File

import akka.actor.{Actor, ActorLogging, Props}
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model._
import week11.model.{ErrorResponse, SuccessfulResponse}

import scala.util.{Failure, Success, Try}


object FilesManager {
  val fileAddress="./src/main/resources/s3"
  val inputFileAddress="./src/main/resources/in"
  val outputFileAddress="./src/main/resources/out/"

  def uploadFile(client:AmazonS3,bucketName:String,key:String,path:String):PutObjectResult={
    val request=new PutObjectRequest(bucketName,key,new File(path))
    client.putObject(request)
  }

  case class GetFile(fileName:String)
  case class UploadFile(filename:String)
  case object UploadFiles
  case object DownloadFiles

  def props(client:AmazonS3,bucketName:String)=Props(new FilesManager(client,bucketName))

}

class FilesManager(client:AmazonS3,bucketName:String) extends  Actor with ActorLogging{
  import FilesManager._

  override def receive:Receive={

    case GetFile(fileName)=>

      val replyTo=sender()
      val key=fileName

      if(client.doesObjectExist(bucketName,key)){
        println("ok")
        val path=s"${fileAddress}/${fileName}"
        val file=new File(path)
        client.getObject(new GetObjectRequest(bucketName,key),file)
        replyTo! Right(SuccessfulResponse(200,s" File ${fileName} lies on paht ${path}e was successfully downloaded "))
        log.info(s"Successfully get file with Name:${fileName}")
      }else{
        replyTo! Left(ErrorResponse(404,s" File with name ${fileName} does not exist "))
        log.info(s"Failed to get file with Name:${fileName}")
      }

    case UploadFile(fileName)=>
      val replyTo=sender()
      val key=fileName

      if(client.doesObjectExist(bucketName,key)){
        replyTo! Left(ErrorResponse(409,s" File with name ${fileName} already exists "))
        log.info(s"Failed to upload file with name ${fileName}")
      }
      else{
        val path=s"${fileAddress}/${fileName}"
        Try(uploadFile(client,bucketName,key,path)) match{
          case Success(_)=>
            replyTo! Right(SuccessfulResponse(201,s" File with name ${fileName} successfully uploaded "))
            log.info(s"Successfully uploaded  ile with Name:${fileName}")
          case Failure(exception)=>
            replyTo! Right(SuccessfulResponse(500,s"Error while uploading file with name ${fileName} "))
            log.info(s"Failed to upload file with name ${fileName}. Error.")
        }
      }

    case UploadFiles=>
      val currentDir: File = new File(outputFileAddress)
      DirectoryAllFiles(currentDir)

      def DirectoryAllFiles(dir: File): Unit = {

        val arrayOfFile=dir.listFiles();
        for(file<-arrayOfFile){
          if(file.isFile){
//            println(file.getName)
            var path=file.getPath
            path=path.substring(outputFileAddress.length,path.length)
            println(path)
            uploadFile(client,bucketName,path,outputFileAddress+"/"+path)
          }else if(file.isDirectory){
//            println(file.getPath)

            DirectoryAllFiles(new File(file.getPath))
          }
        }


      }

    case DownloadFiles=>
    val objects=client.listObjects(new ListObjectsRequest().withBucketName(bucketName))

      objects.getObjectSummaries.forEach(objectSummary=>{
        val file = new File(inputFileAddress+"/"+objectSummary.getKey)
//

        client.getObject(new GetObjectRequest(bucketName,objectSummary.getKey ), file)
      }
      )
  }
}