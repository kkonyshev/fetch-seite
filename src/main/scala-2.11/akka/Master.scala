package akka

import akka.actor.{Actor, Props, Terminated}
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Router}
import model.Trade

class Master extends Actor {

  var router = {
    val routees = Vector.fill(3) {
      val r = context.actorOf(Props[SCIndexWorker])
      context watch r
      ActorRefRoutee(r)
    }
    Router(RoundRobinRoutingLogic(), routees)
  }

  def receive = {
    case w: Trade =>
      router.route(w, sender())
    case Terminated(a) =>
      router = router.removeRoutee(a)
      val r = context.actorOf(Props[SCIndexWorker])
      context watch r
      router = router.addRoutee(r)
  }
}