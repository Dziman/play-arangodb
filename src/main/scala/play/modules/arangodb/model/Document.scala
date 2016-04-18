package play.modules
package arangodb.model

trait Document {
  def _id: String
  def _key: String
  def _rev: String
}

case class Documents(documents: List[String])

case class RemoveDocumentResult(_id: String, _key: String, _rev: String) extends Document
