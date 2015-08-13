import java.text.{ParseException, SimpleDateFormat}
import java.util.Date

import scala.xml.{NodeSeq, Node}

/**
 * Created by ka on 08/08/15.
 */
case class BaseInfo(href: String, title: String, price: String, tradeType: String)
object BaseInfo {
  def create(n: Node): BaseInfo = {
    val root: NodeSeq = n \\ "div" //Search-item-1
    val content: NodeSeq = root \\ "div" //Search-item-content
    val option: NodeSeq = content \\ "div" //Search-item-option
    val href: String = (option(1).child(1) \\ "a" \\ "@href").text.trim
    val title: String = (option(1).child(1) \\ "a").text.trim
    val price: String = (option(3).child(2)).text.trim
    val priceWrapped = price match {
      case x if x.contains("RUB") || x.contains("EUR") || x.contains("USD") => x.replaceAll("\\s", "")
      case x => x
    }
    val tradeType: String = (option.last.child(1) \\ "ul" \\ "li" \\ "span").text.trim

    new BaseInfo(href, title.replaceAll("\\n", "\\s"), priceWrapped, tradeType)
  }
}

case class OrgInfo(status: String, name: String, link: String)
object OrgInfo {
  def create(n: Node): OrgInfo = {
    val root: NodeSeq = n \\ "div" //Search-item-2
    val content: NodeSeq = root \\ "div" //Search-item-content
    val option: NodeSeq = content \\ "div" //Search-item-option
    val status: String = (option(2).child(0)).text.trim.replaceAll("\\s+", " ")
    val name: String = (option(1).child(3).child(3)).text.trim
    val link: String = (option(1).child(3) \\ "a" \\ "@href").text.trim
    new OrgInfo(status, name, link)
  }
}
case class TradeStatus(status: String, publishDate: Date, endDate: Date)
object TradeStatus {
  def create(n: Node): TradeStatus = {
    val root: NodeSeq = n \\ "div" //Search-item-2
    val content: NodeSeq = root \\ "div" //Search-item-content
    val option: NodeSeq = content \\ "div" //Search-item-option

    val status: String = option(2).text.trim
    val publishedDate: String = option(3).child(2).text.trim
    val endDate: String = option(4).child(2).text.trim
    new TradeStatus(status, parseDateSafely(publishedDate), parseDateSafely(endDate))
  }
  def parseDateSafely(stringDate: String) : Date = {
    try {
      new SimpleDateFormat("MM.dd.yyyy hh:mm").parse(stringDate)
    } catch {
      case e: ParseException => new Date
    }
  }
}
case class TradeInfo(views: Int, traders: Int)
object TradeInfo {
  def create(n: Node): TradeInfo = {
    val root: NodeSeq = n \\ "div" //Search-item-2
    val content: NodeSeq = root \\ "div" //Search-item-content
    val option: NodeSeq = content \\ "div" //Search-item-option
    val viewsCount: Int = option(2).child(2).text.trim.toInt
    val tradersCount: Int = option(3).child(2).text.trim.toInt

    new TradeInfo(viewsCount, tradersCount)
  }
}
case class Trade(base: BaseInfo, org: OrgInfo, status: TradeStatus, info: TradeInfo)