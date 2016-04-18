package actors

import akka.actor.Actor
import akka.routing.{AddRoutee, RemoveRoutee, Routee}

/**
  * Created by: MAXIRU 
  * Date: 3/29/16
  * Time: 5:25 PM
  */
class RouterActor extends Actor {
  var routees = Set[Routee]()

  def receive = {
    case ar: AddRoutee => routees = routees + ar.routee
    case rr: RemoveRoutee => routees = routees - rr.routee
    case msg => routees.foreach(_.send(msg, sender))
  }
}
