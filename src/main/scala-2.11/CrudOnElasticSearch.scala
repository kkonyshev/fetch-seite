import org.elasticsearch.action.search.{SearchType, SearchResponse}
import org.elasticsearch.index.query.QueryBuilders

/**
 * Created by ka on 08/08/15.
 */
object CrudOnElasticSearch extends ESOperation with App {

  val client = getClient()
  /*
  val mappingResponse = addMappingToIndex("twitter", client)
  println("@@@@@ mapping response is " + mappingResponse.isAcknowledged())
  val bulkInsertResponse = insertBulkDocument(client)
  println("@@@@@@ number of documents inserted by bulk request  is " + bulkInsertResponse.getItems.length)
  Thread.sleep(800)
  val sortedResult = sortByTimeStamp(client, "twitter")
  println("@@@@@@ sorted by time stamp and returns number filtered document " + sortedResult.getHits.getHits.length)
  val updateResponse = updateIndex(client, "twitter", "tweet", "1")
  println("@@@@@@@@ update response document version is " + updateResponse.getVersion)
  */
  /*val response = client.prepareSearch("fabrikant")
    .setTypes("baseInfoTest")
    .setSearchType(SearchType.DEFAULT)
    //.setQuery("tradeType=Закупка")
    //.setQuery(QueryBuilders.functionScoreQuery().boostMode("martin"))             // Query
    .setQuery(QueryBuilders.regexpQuery("name", ".*ti.*"))
  //.setPostFilter(FilterBuilders.rangeFilter("age").from(12).to(18))   // Filter
    .setExplain(true)
    .execute()
    .actionGet();
  println( response)*/
  val queryBuilder = QueryBuilders.boolQuery().must(QueryBuilders.queryStringQuery("*литр*").field("title"))
  val req = client.prepareSearch("fabrikant")
    .setTypes("baseInfo")
    .setSearchType(SearchType.DEFAULT).setQuery(queryBuilder)
    .setSearchType(SearchType.DFS_QUERY_THEN_FETCH);

  val res = req.setExplain(true).execute().actionGet();
  println(res)
  //val deleteDocument = deleteDocumentById(client, "twitter", "tweet", "1")
  //println("@@@@@ deleted document by id is " + deleteDocument.isFound())
  //val deleteIndexResponse = deleteIndex(client, "twitter")
  //println("@@@@@ delete index response " + deleteIndexResponse)
}
