
import java.text.{ParseException, SimpleDateFormat}
import java.util.concurrent.TimeUnit
import akka.Master
import akka.actor.{PoisonPill, Props, Actor, ActorSystem}
import akka.dispatch.Futures
import akka.routing.{RoundRobinRoutingLogic, Router, ActorRefRoutee}
import model._
import org.ccil.cowan.tagsoup.jaxp.SAXFactoryImpl
import org.elasticsearch.client.Client
import org.elasticsearch.common.xcontent.{XContentFactory, XContentBuilder}
import spray.http._
import spray.client.pipelining._
import utils.es.ESOperation

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise, Future}
import scala.io.{Codec, Source}
import scala.util.{Failure, Success}
import scala.xml.factory.XMLLoader
import scala.xml.{Elem, NodeSeq, XML, Node}

/**
 *
 * Created by konishev on 18/06/2015.
 */
object FetchSite extends ESOperation {


  implicit val system = ActorSystem()
  import system.dispatcher // execution context for futures

  def main2 (args: Array[String]) {
    val price = "447 552.85 RUB"
    val priceWrapped = price match {
      case x if x.contains("RUB") => x.replaceAll("\\s", "")
      case x => x
    }
    println(priceWrapped)
  }

  def mainB(args: Array[String]) {
    fetchCommonSite("https://www.fabrikant.ru/trades/corporate/ProcedurePurchase/?action=view&id=3974#lot_1", process2)
  }

  def process2(site: String) = {
    val saxParser: XMLLoader[Elem] = XML.withSAXParser((new SAXFactoryImpl).newSAXParser)
    val content = saxParser.loadString(site)

    def actorWithRole(n: Node) = n \\ "@class" xml_sameElements(List("fname"))

    val goodRows = content \\ "tr" filter actorWithRole

    val e = content \\ "table" filter ((node: xml.Node) => (node \\ "@style").text == "width:100%;")
    println("===")
    var i = 0;
    for (nn <-e(0).child(0).child) {
      println(i + ": " + nn)
      i=i+1
    }
  }


  def main (args: Array[String]) {
    val helloActor = system.actorOf(Props[Master], name = "master")
    println(helloActor.path)
    val f = Future {
      for (i <- 37.to(50)) {
        fetchSite(i, processSitePage)
      }
    }

    //Await.result(f, Duration.apply(10, TimeUnit.SECONDS))
    //system.shutdown()
    //fetchCommonSite("https://www.fabrikant.ru/trades/corporate/ProcedurePurchase/?action=view&id=3974#lot_1", process2)
  }

  def processSitePage(site: String) = {
    val saxParser: XMLLoader[Elem] = XML.withSAXParser((new SAXFactoryImpl).newSAXParser)
    val content =  saxParser.loadString(site)
    println("total count: " + getCount(content))

    for (div <- content \\ "div") {
      val divClassAttribute: Option[Seq[Node]] = div.attribute("class")
      if (divClassAttribute.isDefined) {
        if (divClassAttribute.get.toString().equalsIgnoreCase("Search-result-item")) {
          val wrapper: NodeSeq = div \\ "div"//Search-wrapper
          val itemWrapper: Node = wrapper(1)

          val t = new Trade(
          BaseInfo.create(itemWrapper.child(1)),
          OrgInfo.create(itemWrapper.child(3)),
          TradeStatus.create(itemWrapper.child(5)),
          TradeInfo.create(itemWrapper.child(7)))

          system.actorSelection("akka://default/user/master") ! t
          //router.route(t, Actor.noSender)
        }
      }
    }
  }



  def getCount(content: Node): Integer = {
    for (div <- content \\ "div") {
      val divClassAttribute: Option[Seq[Node]] = div.attribute("class")
      if (divClassAttribute.isDefined) {
        if (divClassAttribute.get.toString().equalsIgnoreCase("Search-result-count")) {
          return (div\\"span").text.toInt
        }
      }
    }
    return 0
  }

  //val proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("proxy.ftc.ru", 3128))

  def fetchSite(pageNumber: Int=1, func: (String) => Unit) = {
    fetchCommonSite("https://www.fabrikant.ru/trades/procedure/search/?page="+pageNumber, func)
  }

  def fetchCommonSite(targetUrl: String, func: (String) => Unit) = {

    //val fileContents = Source.fromFile("ex.html").getLines.mkString


    val pipeline: HttpRequest => Future[HttpResponse] = sendReceive

    val response: Future[HttpResponse] = pipeline(Get(targetUrl))


    response onComplete{
      case Failure(ex) => ex.printStackTrace()
        system.shutdown()
        //func.apply(Source.fromFile("src/main/resources/dummy.html").getLines.mkString)
      case Success(resp) => println("success for: " + targetUrl)
        val respSource: String = resp.message.entity.asString
        //printToFile(new File("1.html")) { p => respSource.toString }
        func.apply(respSource)
        //system.shutdown()
    }

    "ok"
  }

  def printToFile(f: java.io.File)(op: java.io.PrintWriter => Unit) {
    val p = new java.io.PrintWriter(f)
    try { op(p) } finally { p.close() }
  }
}





