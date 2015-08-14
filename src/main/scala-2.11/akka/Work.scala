package akka

import model.Trade

import scala.xml.Node

/**
 * Created by ka on 14/08/15.
 */
case class WorkIndex(t: Trade);
case class WorkParseTradeListPage(n: Node);
