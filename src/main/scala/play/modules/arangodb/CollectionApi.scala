package play.modules
package arangodb

import javax.inject.Inject

import play.api.libs.json._
import play.modules.arangodb.model._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * TODO Document
  */
trait CollectionApi {
  def info(name: String): Future[Option[Collection]]

  def properties(name: String): Future[Option[CollectionProperties]]

  def documentsCount(name: String): Future[Option[Long]]

  def statistics(name: String): Future[Option[CollectionProperties]]

  def revision(name: String): Future[Option[Collection]]

  def checksum(name: String, withRevisions: Boolean = false, withData: Boolean = false): Future[Option[Collection]]

  def all(withSystem: Boolean = false): Future[Option[Collections]]

  def create(collectionProperties: CollectionCreateProperties): Future[Option[Collection]]

  def drop(name: String): Future[Option[String]]

  def truncate(name: String): Future[Option[Collection]]

  def load(name: String): Future[Option[Collection]]

  def unload(name: String): Future[Option[Collection]]

  def rename(name: String, newName: String): Future[Option[Collection]]

  def rotate(name: String): Future[Option[Boolean]]

  def setWaitForSync(name: String, waitForSync: Boolean): Future[Option[CollectionProperties]]

  def setJournalSize(name: String, journalSize: Long): Future[Option[CollectionProperties]]
}

final class RestCollectionApi @Inject()(requestExecutor: RequestExecutor) extends CollectionApi {
  override def info(name: String): Future[Option[Collection]] = {
    requestExecutor.execute[Collection](url = s"collection/$name")
  }

  override def documentsCount(name: String): Future[Option[Long]] = {
    requestExecutor.execute[CollectionProperties](
      url = s"collection/$name/count"
    ).map { properties =>
      properties.flatMap { p => p.count }
    }
  }

  override def properties(name: String): Future[Option[CollectionProperties]] = {
    requestExecutor.execute[CollectionProperties](url = s"collection/$name/properties")
  }


  override def statistics(name: String): Future[Option[CollectionProperties]] = {
    requestExecutor.execute[CollectionProperties](url = s"collection/$name/figures")
  }

  override def revision(name: String): Future[Option[Collection]] = {
    requestExecutor.execute[Collection](url = s"collection/$name/revision")
  }

  override def checksum(name: String, withRevisions: Boolean = false, withData: Boolean = false): Future[Option[Collection]] = {
    requestExecutor.execute[Collection](
      url = s"collection/$name/checksum",
      query = List(
        ("withRevisions", s"$withRevisions"),
        ("withData", s"$withData")
      )
    )
  }

  override def all(withSystem: Boolean = false): Future[Option[Collections]] = {
    requestExecutor.execute[Collections](
      url = "collection",
      query = List(("excludeSystem", s"${!withSystem}"))
    )
  }

  override def drop(name: String): Future[Option[String]] = {
    def successHandler(body: Option[JsValue], headers: Map[String, Seq[String]]) = body map { json => (json \ "id").as[String] }
    requestExecutor.execute[String](
      method = "DELETE",
      url = s"collection/$name",
      handlers = List((200,  successHandler))
    )
  }

  override def truncate(name: String): Future[Option[Collection]] = {
    requestExecutor.execute[Collection](method = "PUT", url = s"collection/$name/truncate")
  }

  override def create(collectionProperties: CollectionCreateProperties): Future[Option[Collection]] = {
    requestExecutor.executeWithBody[Collection, CollectionCreateProperties](
      url = s"collection",
      body = collectionProperties
    )
  }

  override def load(name: String): Future[Option[Collection]] = {
    requestExecutor.execute[Collection](method = "PUT", url = s"collection/$name/load")
  }

  override def unload(name: String): Future[Option[Collection]] = {
    requestExecutor.execute[Collection](method = "PUT", url = s"collection/$name/unload")
  }

  override def rename(name: String, newName: String): Future[Option[Collection]] = {
    requestExecutor.executeWithBody[Collection, JsObject](
      method = "PUT",
      url = s"collection/$name/rename",
      body = Json.obj("name" -> newName),
      handlers = List((409, (_, _) => None))
    )
  }

  override def rotate(name: String): Future[Option[Boolean]] = {
    def successHandler(body: Option[JsValue], headers: Map[String, Seq[String]]) = body map { json => (json \ "result").as[Boolean] }
    def noJournalHandler(body: Option[JsValue], headers: Map[String, Seq[String]]) = body map  { json => !(json \ "errorNum").asOpt[Int].contains(1105) }
    requestExecutor.execute[Boolean](
      method = "PUT",
      url = s"collection/$name/rotate",
      handlers = List(
        (200, successHandler),
        (400, noJournalHandler)
      )
    )
  }

  override def setWaitForSync(name: String, waitForSync: Boolean): Future[Option[CollectionProperties]] = {
    requestExecutor.executeWithBody[CollectionProperties, JsObject](
      method = "PUT",
      url = s"collection/$name/properties",
      body = Json.obj("waitForSync" -> waitForSync)
    )
  }

  override def setJournalSize(name: String, journalSize: Long): Future[Option[CollectionProperties]] = {
    requestExecutor.executeWithBody[CollectionProperties, JsObject](
      method = "PUT",
      url = s"collection/$name/properties",
      body = Json.obj("journalSize" -> journalSize)
    )
  }
}
