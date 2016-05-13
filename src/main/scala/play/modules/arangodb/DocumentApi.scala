package play.modules
package arangodb

import javax.inject.Inject

import play.api.libs.json.{JsValue, Reads, Writes}
import play.modules.arangodb.model._

import scala.concurrent.Future

trait DocumentApi {
  def read[D](handle: String, revision: Option[Either[String, String]] = None)(implicit objectReads: Reads[D]): Future[Either[ArangoError, D]]

  def create[D](collection: String, doc: D, waitForSync: Option[Boolean] = None)(implicit objectWrites: Writes[D]): Future[Either[ArangoError, DocumentResult]]

  def replace[D](
                  handle: String,
                  doc: D, revision: Option[String] = None,
                  force: Boolean = false,
                  waitForSync: Option[Boolean] = None
                )
                (implicit objectWrites: Writes[D]): Future[Either[ArangoError, DocumentResult]]

  def patch[D](
                handle: String,
                doc: D,
                keepNull: Boolean = false,
                mergeObjects: Boolean = true,
                revision: Option[String] = None,
                force: Boolean = false,
                waitForSync: Option[Boolean] = None
              )
              (implicit objectWrites: Writes[D]): Future[Either[ArangoError, DocumentResult]]

  def remove(handle: String, revision: Option[String] = None): Future[Either[ArangoError, DocumentResult]]

  def revision(handle: String): Future[Either[ArangoError, String]]

  def all(collection: String, returnIds: Boolean = false): Future[Either[ArangoError, Documents]]
}

final class RestDocumentApi @Inject()(requestExecutor: RequestExecutor) extends DocumentApi {

  override def read[D](handle: String, revision: Option[Either[String, String]] = None)(implicit objectReads: Reads[D]): Future[Either[ArangoError, D]] = {
    def notModifiedHandler(body: Option[JsValue], headers: Map[String, Seq[String]]) = Left(ArangoError(304, 0, "Not modified"))
    val headers = revision.fold(Seq.empty[(String, String)]) { docRevision =>
      docRevision.fold(
        noneMatchRevision => List(("If-None-Match", noneMatchRevision)),
        matchRevision => List(("If-Match", matchRevision))
      )
    }
    requestExecutor.execute[D](
      method = "GET",
      url = s"document/$handle",
      headers = headers,
      handlers = List((304, notModifiedHandler))
    )
  }

  override def create[D](collection: String, doc: D, waitForSync: Option[Boolean])(implicit objectWrites: Writes[D]): Future[Either[ArangoError, DocumentResult]] = {
    val waitForSyncQuery = waitForSync.fold(Seq.empty[(String, String)])(wait => List(("waitForSync", s"$waitForSync")))
    requestExecutor.executeWithBody[DocumentResult, D](
      url = "document",
      query = List(("collection", collection)) ++ waitForSyncQuery,
      body = doc,
      handlers = List(
        (201, requestExecutor.defaultSuccessHandler[DocumentResult]),
        (202, requestExecutor.defaultSuccessHandler[DocumentResult])
      )
    )
  }

  override def replace[D](handle: String, doc: D, revision: Option[String] = None, force: Boolean = false, waitForSync: Option[Boolean] = None)
                         (implicit objectWrites: Writes[D]): Future[Either[ArangoError, DocumentResult]] = {
    val waitForSyncQuery = waitForSync.fold(Seq.empty[(String, String)])(wait => List(("waitForSync", s"$waitForSync")))
    val policyQuery = List(("policy", s"${if (force) "last" else "error"}"))
    val headers = revision.fold(Seq.empty[(String, String)])(docRevision => List(("If-Match", docRevision)))
    requestExecutor.executeWithBody[DocumentResult, D](
      method = "PUT",
      url = s"document/$handle",
      headers = headers,
      query = waitForSyncQuery ++ policyQuery,
      body = doc,
      handlers = List(
        (201, requestExecutor.defaultSuccessHandler[DocumentResult]),
        (202, requestExecutor.defaultSuccessHandler[DocumentResult])
      )
    )
  }

  override def patch[D](
                           handle: String,
                           doc: D,
                           keepNull: Boolean = false,
                           mergeObjects: Boolean = true,
                           revision: Option[String] = None,
                           force: Boolean = false,
                           waitForSync: Option[Boolean] = None
                         )
                         (implicit objectWrites: Writes[D]): Future[Either[ArangoError, DocumentResult]] = {
    val waitForSyncQuery = waitForSync.fold(Seq.empty[(String, String)])(wait => List(("waitForSync", s"$waitForSync")))
    val policyQuery = List(("policy", s"${if (force) "last" else "error"}"))
    val headers = revision.fold(Seq.empty[(String, String)])(docRevision => List(("If-Match", docRevision)))
    requestExecutor.executeWithBody[DocumentResult, D](
      method = "PATCH",
      url = s"document/$handle",
      headers = headers,
      query = List(("keepNull", s"$keepNull"), ("mergeObjects", s"$mergeObjects")) ++ waitForSyncQuery ++ policyQuery,
      body = doc,
      handlers = List(
        (201, requestExecutor.defaultSuccessHandler[DocumentResult]),
        (202, requestExecutor.defaultSuccessHandler[DocumentResult])
      )
    )
  }

  override def remove(handle: String, revision: Option[String] = None): Future[Either[ArangoError, DocumentResult]] = {
    val headers = revision.fold(Seq.empty[(String, String)])(docRevision => List(("If-Match", docRevision)))
    requestExecutor.execute[DocumentResult](
      method = "DELETE",
      url = s"document/$handle",
      headers = headers,
      handlers = List((202, requestExecutor.defaultSuccessHandler[DocumentResult]))
    )
  }

  override def revision(handle: String): Future[Either[ArangoError, String]] = {
    def successHandler(body: Option[JsValue], headers: Map[String, Seq[String]]) = {
      headers get "Etag" match {
        case Some(etagHeader) => Right(etagHeader.head)
        case None => Left(ArangoError(500, 0, "Empty response"))
      }
    }
    requestExecutor.execute[String](
      method = "HEAD",
      url = s"document/$handle",
      handlers = List((200, successHandler))
    )
  }

  override def all(collection: String, returnIds: Boolean = false): Future[Either[ArangoError, Documents]] = {
    val returnType = if (returnIds) "id" else "key"
    requestExecutor.execute[Documents](
      url = "document",
      query = List(("collection", collection), ("type", returnType))
    )
  }
}
