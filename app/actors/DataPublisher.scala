package actors

import actors.DataPublisher.Publish
import akka.actor.{Actor, ActorRef}
import akka.routing.{ActorRefRoutee, AddRoutee, RemoveRoutee}
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.{Cancel, Request}
import models.ClientPayload

import scala.collection.mutable

/**
  * Created by: MAXIRU 
  * Date: 3/29/16
  * Time: 11:47 AM
  */
case class InitialCount(count: Int)

class DataPublisher(router: ActorRef) extends ActorPublisher[ClientPayload] {
  var queue: mutable.Queue[ClientPayload] = mutable.Queue()
  var initialCount = 0

  // on startup, register with routee
  override def preStart() {
    router ! AddRoutee(ActorRefRoutee(self))
  }

  // cleanly remove this actor from the router. To
  // make sure our custom router only keeps track of
  // alive actors.
  override def postStop(): Unit = {
    router ! RemoveRoutee(ActorRefRoutee(self))
  }

  override def receive: Actor.Receive = {
    case Publish(s) =>
      println("publish event received")
      queue.enqueue(s)
      publishIfNeeded()
    case Request(cnt) =>
      publishIfNeeded()
    case InitialCount(n) => initialCount = n
    case Cancel => context.stop(self)
    case _ =>
      println("dunno")
  }

  def publishIfNeeded() = {
    while (queue.nonEmpty && isActive && totalDemand > 0) {
      onNext(queue.dequeue())
    }
  }
}

object DataPublisher {

  case class Publish(steps: ClientPayload)

}
