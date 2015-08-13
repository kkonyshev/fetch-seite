
import java.io.File
import java.net.InetSocketAddress
import java.nio.file.Files
import java.text.{ParseException, SimpleDateFormat}
import java.util.Date
import akka.actor.ActorSystem
import org.ccil.cowan.tagsoup.jaxp.SAXFactoryImpl
import org.elasticsearch.common.xcontent.{XContentFactory, XContentBuilder}
import spray.http._
import spray.client.pipelining._

import scala.concurrent.Future
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

  def mainA (args: Array[String]) {
    val client = getClient()

    val y = XContentFactory.jsonBuilder()
      .startObject()
      .field("href", "http://ya.ru")
      .field("title", "some long title ann short desreption")
      .field("price", "10.00")
      .field("name", "Martin")
      .endObject()

    val bulkRequest = client.prepareBulk()
    bulkRequest.add(client.prepareIndex("fabrikant", "baseInfoTest", "1").setSource(y))
    println("added: " + bulkRequest.execute().actionGet().getItems.length)
    system.shutdown()
  }

  def main (args: Array[String]) {

    for (i<-1.to(1))
    {
      fetchSite(i, processSitePage)
    }

    /*
    println (x)

    val id = x.href.substring(x.href.lastIndexOf("=")+1)
    val client = getClient()

    val y = XContentFactory.jsonBuilder()
      .startObject()
      .field("href", x.href)
      .field("title", x.title)
      .field("price", x.price)
      .field("tradeType", x.tradeType)
      .endObject()

    val bulkRequest = client.prepareBulk()
    bulkRequest.add(client.prepareIndex("fabrikant", "baseInfo", id).setSource(y))
    println("added: " + bulkRequest.execute().actionGet().getItems.length)
    */

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
          println(t)

          val id = t.base.href.substring(t.base.href.lastIndexOf("=")+1)
          val client = getClient()
          val bulkRequest = client.prepareBulk()

          val y = XContentFactory.jsonBuilder()
            .startObject()
            .field("href", t.base.href)
            .field("title", t.base.title)
            .field("price", t.base.price)
            .field("tradeType", t.base.tradeType)
            .endObject()

          bulkRequest.add(client.prepareIndex("fabrikant", "trades", id).setSource(y))
          println("added: " + bulkRequest.execute().actionGet().getItems.length)
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
        system.shutdown()
        //printToFile(new File("1.html")) { p => respSource.toString }
        func.apply(respSource)
    }

  }

  def printToFile(f: java.io.File)(op: java.io.PrintWriter => Unit) {
    val p = new java.io.PrintWriter(f)
    try { op(p) } finally { p.close() }
  }
}



