package akka

import akka.actor.{Actor, PoisonPill}
import model.Trade
import org.elasticsearch.client.Client
import utils.es.ESOperation
import utils.json.MarshallableImplicits
import utils.json.MarshallableImplicits._


class SCIndexWorker extends Actor with ESOperation {

  val client: Client = getClient()

  def receive = {
    case t: Trade => {
      val href: String = t.base.href
      val id = href.substring(href.lastIndexOf("=")+1)
      println(t)
      client.prepareIndex("fabrikant", "trades", id).setSource(t.toJson).execute()
    }
    case p: PoisonPill => {
      println("down...")
      context.system.shutdown()
    }
    case _ => println("huh?")
  }

}
