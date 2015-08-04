
import java.net.InetSocketAddress

import akka.actor.ActorSystem
import org.ccil.cowan.tagsoup.jaxp.SAXFactoryImpl
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
object FetchSite {

  def main2 (args: Array[String]) {
    val price = "447 552.85 RUB"
    val priceWrapped = price match {
      case x if x.contains("RUB") => x.replaceAll("\\s", "")
      case x => x
    }
    println(priceWrapped)
  }

  def main1(args: Array[String]) {
    //val site = fetchCommonSite("https://www.fabrikant.ru/trades/atom/PriceMonitoring/?action=view&id=2831")
    //println(site)
  }

  def main (args: Array[String]) {

    for (i<-1.to(2))
    {
      fetchSite(i, process)
    }
  }



  def process(site: String) = {
    //println(site)
    val saxParcer: XMLLoader[Elem] = XML.withSAXParser((new SAXFactoryImpl).newSAXParser)
    val content =  saxParcer.loadString(site)
    println("total count: " + getCount(content))

    for (div <- content \\ "div") {
      val divClassAttribute: Option[Seq[Node]] = div.attribute("class")
      if (divClassAttribute.isDefined) {
        if (divClassAttribute.get.toString().equalsIgnoreCase("Search-result-item")) {
          val wrapper: NodeSeq = div \\ "div" //Search-wrapper
          //println("NODE_dump: " + wrapper(0).child(1))
          for (n <- wrapper.iterator) {
            val classAttribute: Option[Seq[Node]] = n.attribute("class")
            if (classAttribute.isDefined) {
              val classAttributeValue: String = classAttribute.get.toString()
              if (classAttributeValue.equalsIgnoreCase("Search-item-1")) {
                println("---base-info---")
                val root: NodeSeq = n \\ "div" //Search-item-1
                val content: NodeSeq = root \\ "div" //Search-item-content
                val option: NodeSeq = content \\ "div" //Search-item-option
                println("href: " + (option(1).child(1) \\ "a" \\ "@href").text.trim)
                println("title: " + (option(1).child(1) \\ "a").text.trim)
                val price: String = (option(3).child(2)).text.trim
                val priceWrapped = price match {
                  case x if x.contains("RUB") => x.replaceAll("\\s", "")
                  case x => x
                }
                println("price: " + priceWrapped)
                println("type: " + (option.last.child(1) \\ "ul" \\ "li" \\ "span").text.trim)
              }
              else if (classAttributeValue.equalsIgnoreCase("Search-item-2")) {
                println("---org-info---")
                val root: NodeSeq = n \\ "div" //Search-item-2
                val content: NodeSeq = root \\ "div" //Search-item-content
                val option: NodeSeq = content \\ "div" //Search-item-option
                println("status: " + (option(2).child(0)).text.trim.replaceAll("\\s+", " "))
                println("ogr name: " + (option(1).child(3).child(3)).text.trim)
                println("ogr link: " + (option(1).child(3) \\ "a" \\ "@href").text.trim)
              }
              else if (classAttributeValue.equalsIgnoreCase("Search-item-3")) {
                println("---status-info---")
                val root: NodeSeq = n \\ "div" //Search-item-2
                val content: NodeSeq = root \\ "div" //Search-item-content
                val option: NodeSeq = content \\ "div" //Search-item-option

                println("trade status: " + option(2).text.trim)
                println("publish date: " + option(3).child(2).text.trim)
                println("end date: " + option(4).child(2).text.trim)
              }
              else if (classAttributeValue.equalsIgnoreCase("Search-item-4")) {
                println("---traders-info---")
                val root: NodeSeq = n \\ "div" //Search-item-2
                val content: NodeSeq = root \\ "div" //Search-item-content
                val option: NodeSeq = content \\ "div" //Search-item-option

                println("views count: " + option(2).child(2).text.trim)
                println("traders count: " + option(3).child(2).text.trim)
                println
              }
            }
          }
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
    implicit val system = ActorSystem()
    import system.dispatcher // execution context for futures

    val pipeline: HttpRequest => Future[HttpResponse] = sendReceive

    val response: Future[HttpResponse] = pipeline(Get(targetUrl))

    val r = response onComplete{
      case Failure(ex) => ex.printStackTrace()
      case Success(resp) => println("success for: " + targetUrl)
        func.apply(resp.message.entity.asString)
    }
  }
}
