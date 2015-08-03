import java.net.{InetSocketAddress, Proxy, URL}

import org.ccil.cowan.tagsoup.jaxp.SAXFactoryImpl

import scala.io.Source
import scala.xml.factory.XMLLoader
import scala.xml.{Elem, NodeSeq, XML, Node}
/**
 *
 * Created by konishev on 18/06/2015.
 */
object FetchSite {

  def proc(node: Node): String =
    node match {
      case <body>{ txt }</body> => "Partial content: " + txt
      case _ => "grmpf"
    }

  def main2 (args: Array[String]) {
    val price = "447 552.85 RUB"
    val priceWrapped = price match {
      case x if x.contains("RUB") => x.replaceAll("\\s", "")
      case x => x
    }
    println(priceWrapped)
  }

  def main (args: Array[String]) {

    for (i<-1.to(3))
    {
      process(fetchSite(i))
    }
  }

  val saxParcer: XMLLoader[Elem] = XML.withSAXParser((new SAXFactoryImpl).newSAXParser)

  def process(site: String): Unit = {
    println(site)
    val content =  saxParcer.loadString(site)
    println("total count: " + getCount(content))

    for (div <- content \\ "div") {
      val divClassAttribute: Option[Seq[Node]] = div.attribute("class")
      if (divClassAttribute.isDefined) {
        if (divClassAttribute.get.toString().equalsIgnoreCase("Search-result-item")) {
          val wrapper: NodeSeq = div \\ "div" //Search-wrapper
          println("NODE_dump: " + wrapper(0).child(1))
          for (n <- wrapper.iterator) {
            if (n.attribute("class").isDefined) {
              if (n.attribute("class").get.toString().equalsIgnoreCase("Search-item-1")) {
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
              else if (n.attribute("class").get.toString().equalsIgnoreCase("Search-item-2")) {
                println("---org-info---")
                val root: NodeSeq = n \\ "div" //Search-item-2
                val content: NodeSeq = root \\ "div" //Search-item-content
                val option: NodeSeq = content \\ "div" //Search-item-option
                println("status: " + (option(2).child(0)).text.trim.replaceAll("\\s+", " "))
                println("ogr name: " + (option(1).child(3).child(3)).text.trim)
                println("ogr link: " + (option(1).child(3) \\ "a" \\ "@href").text.trim)
              }
              else if (n.attribute("class").get.toString().equalsIgnoreCase("Search-item-3")) {
                println("---status-info---")
                val root: NodeSeq = n \\ "div" //Search-item-2
                val content: NodeSeq = root \\ "div" //Search-item-content
                val option: NodeSeq = content \\ "div" //Search-item-option

                println("trade status: " + option(2).text.trim)
                println("publish date: " + option(3).child(2).text.trim)
                println("end date: " + option(4).child(2).text.trim)
              }
              else if (n.attribute("class").get.toString().equalsIgnoreCase("Search-item-4")) {
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

  val proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("proxy.ftc.ru", 3128))

  def fetchSite(pageNumber: Int=1) = {
    val url = new URL("https://www.fabrikant.ru/trades/procedure/search/?page="+pageNumber)
    val connection = url.openConnection()//url.openConnection(proxy)
    val stream = Source.fromInputStream(connection.getInputStream)
    val page = stream.getLines().map(_.toString).mkString
    stream.close()
    page
  }
}
