package week9My.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import week9My.model.{ErrorResponse, SuccessfulResponse}
import week9My.model.{Author, Book}

object TestBot {
  case object  TestCreate

  case object TestConflict

  case object TestRead

  case object TestNotFound
  case object TestUpdate
  case object TestDelete

  def props(manager:ActorRef) = Props(new TestBot(manager))
}

class TestBot(manager:ActorRef) extends Actor with ActorLogging{
  import TestBot._
  override def receive:Receive={
    case TestCreate=>
      manager! LibraryManager.CreateBook(Book("id-1", "Princess", Author("dir-1", "Todd", "Philips","12/12/1999","22/10/2007"), 2019,365,"Roman"))

    case TestConflict =>
      manager ! LibraryManager.CreateBook(Book("id-1", "Princess", Author("dir-1", "Todd", "Philips","12/12/1999","22/10/2007"), 2019,365,"Roman"))
      manager ! LibraryManager.CreateBook(Book("id-1", "Princess", Author("dir-1", "Todd", "Philips","12/12/1999","22/10/2007"), 2019,365,"Roman"))

    case TestRead =>
      manager ! LibraryManager.ReadBook("1")

    case TestUpdate=>
      manager ! LibraryManager.UpdateBook(Book("id-1", "Princess", Author("dir-1", "Todd", "Philips","12/12/1999","22/10/2007"), 2019,365,"Roman"))

    case TestNotFound =>
    manager ! LibraryManager.DeleteBook("3")

    case TestDelete=>
      manager ! LibraryManager.DeleteBook("1")

    case SuccessfulResponse(status, msg) =>
      log.info("Received Successful Response with status: {} and message: {}", status, msg)

    case ErrorResponse(status, msg) =>
      log.warning("Received Error Response with status: {} and message: {}", status, msg)

    case book: Book =>
      log.info("Received book: [{}]", book)
  }
}