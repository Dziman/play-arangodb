package play.modules.arangodb

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.libs.json._

package object model {
  // General JSON Reads
  implicit val dateReads = new Reads[DateTime] {
    val dateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")

    override def reads(js: JsValue): JsResult[DateTime] = {
      JsSuccess(DateTime.parse(js.as[String], dateFormatter))
    }
  }
  implicit val errorJsonReads = Json.reads[ArangoError]

  // JSON Reads/Writes for Collection API
  implicit val collectionJsonReads = Json.reads[Collection]
  implicit val collectionsJsonReads = Json.reads[Collections]
  implicit val compactionStatusJsonReads = Json.reads[CompactionStatus]
  implicit val figureJsonReads = Json.reads[Figure]
  implicit val figuresJsonReads = Json.reads[Figures]
  implicit val keyOptionsJsonReads = Json.reads[KeyOptions]
  implicit val collectionPropertiesJsonReads = Json.reads[CollectionProperties]

  implicit val keyOptionsJsonWrites = Json.writes[KeyOptions]
  implicit val collectionCreatePropertiesJsonWrites = Json.writes[CollectionCreateProperties]

  //JSON Reads/Writes for Document API
  implicit val documentsJsonReads = Json.reads[Documents]
  implicit val removeDocumentResultJsonReads = Json.reads[RemoveDocumentResult]
}
