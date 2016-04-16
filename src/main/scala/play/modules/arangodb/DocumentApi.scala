package play.modules
package arangodb

import javax.inject.Inject

import play.api.libs.json.JsValue
import play.modules.arangodb.model._

import scala.concurrent.Future

trait DocumentApi {
//  def read(collection: String, key: String) = read(s"$collection/$key")
//
  def read[D <: Document](handle: String): Future[Option[D]]
//
//  def create[D <: Document](handle: String, doc: D): Future[Option[D]]
//
//  def replace[D <: Document](handle: String, doc: D): Future[Option[D]]
//
//  def patch[P, D <: P](handle: String, patch: P): Future[Option[D]]

  def remove(handle: String, revision: Option[String] = None): Future[Option[RemoveDocumentResult]]

  def revision(handle: String): Future[Option[String]]

  def all(collection: String, returnIds: Boolean = false): Future[Option[Documents]]
}

final class RestDocumentApi @Inject()(requestExecutor: RequestExecutor) extends DocumentApi {

  override def read[D <: Document](handle: String): Future[Option[D]] = {
    requestExecutor.execute[D](
      method = "GET",
      url = s"document/$handle"
    )
  }

  override def remove(handle: String, revision: Option[String] = None): Future[Option[RemoveDocumentResult]] = {
    val headers = revision.fold(Seq.empty[(String, String)])(docRevision => List(("If-Match", docRevision)))
    requestExecutor.execute[RemoveDocumentResult](
      method = "DELETE",
      url = s"document/$handle",
      headers = headers,
      handlers = List(
        (202, requestExecutor.defaultSuccessHandler[RemoveDocumentResult]),
        (412, requestExecutor.defaultSuccessHandler[RemoveDocumentResult])
      )
    )
  }

  override def revision(handle: String): Future[Option[String]] = {
    def successHandler(body: Option[JsValue], headers: Map[String, Seq[String]]) = headers get "Etag" map {etagHeader => etagHeader.head}
    requestExecutor.execute[String](
      method = "HEAD",
      url = s"document/$handle",
      handlers = List((200, successHandler))
    )
  }

  override def all(collection: String, returnIds: Boolean = false): Future[Option[Documents]] = {
    val returnType = if (returnIds) "id" else "key"
    requestExecutor.execute[Documents](
      url = "document",
      query = List(("collection", collection), ("type", returnType))
    )
  }
}
