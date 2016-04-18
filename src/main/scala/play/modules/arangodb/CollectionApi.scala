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
  def info(name: String): Future[Either[ArangoError, Collection]]

  def properties(name: String): Future[Either[ArangoError, CollectionProperties]]

  def documentsCount(name: String): Future[Either[ArangoError, Long]]

  def statistics(name: String): Future[Either[ArangoError, CollectionProperties]]

  def revision(name: String): Future[Either[ArangoError, Collection]]

  def checksum(name: String, withRevisions: Boolean = false, withData: Boolean = false): Future[Either[ArangoError, Collection]]

  def all(withSystem: Boolean = false): Future[Either[ArangoError, Collections]]

  def create(collectionProperties: CollectionCreateProperties): Future[Either[ArangoError, Collection]]

  def drop(name: String): Future[Either[ArangoError, String]]

  def truncate(name: String): Future[Either[ArangoError, Collection]]

  def load(name: String): Future[Either[ArangoError, Collection]]

  def unload(name: String): Future[Either[ArangoError, Collection]]

  def rename(name: String, newName: String): Future[Either[ArangoError, Collection]]

  def rotate(name: String): Future[Either[ArangoError, Boolean]]

  def setWaitForSync(name: String, waitForSync: Boolean): Future[Either[ArangoError, CollectionProperties]]

  def setJournalSize(name: String, journalSize: Long): Future[Either[ArangoError, CollectionProperties]]
}

final class RestCollectionApi @Inject()(requestExecutor: RequestExecutor) extends CollectionApi {
  override def info(name: String): Future[Either[ArangoError, Collection]] = {
    requestExecutor.execute[Collection](url = s"collection/$name")
  }

  override def documentsCount(name: String): Future[Either[ArangoError, Long]] = {
    requestExecutor.execute[CollectionProperties](
      url = s"collection/$name/count"
    ).map { eitherErrorOrCount =>
      eitherErrorOrCount.fold(
          error => Left(error),
          properties => properties.count match {
            case Some(count) => Right(count)
            case None =>Left(ArangoError(500, 0, "Unexpected response"))
          }
        )

    }
  }

  override def properties(name: String): Future[Either[ArangoError, CollectionProperties]] = {
    requestExecutor.execute[CollectionProperties](url = s"collection/$name/properties")
  }


  override def statistics(name: String): Future[Either[ArangoError, CollectionProperties]] = {
    requestExecutor.execute[CollectionProperties](url = s"collection/$name/figures")
  }

  override def revision(name: String): Future[Either[ArangoError, Collection]] = {
    requestExecutor.execute[Collection](url = s"collection/$name/revision")
  }

  override def checksum(name: String, withRevisions: Boolean = false, withData: Boolean = false): Future[Either[ArangoError, Collection]] = {
    requestExecutor.execute[Collection](
      url = s"collection/$name/checksum",
      query = List(
        ("withRevisions", s"$withRevisions"),
        ("withData", s"$withData")
      )
    )
  }

  override def all(withSystem: Boolean = false): Future[Either[ArangoError, Collections]] = {
    requestExecutor.execute[Collections](
      url = "collection",
      query = List(("excludeSystem", s"${!withSystem}"))
    )
  }

  override def drop(name: String): Future[Either[ArangoError, String]] = {
    def successHandler(body: Option[JsValue], headers: Map[String, Seq[String]]) = {
      body match {
        case Some(json) => Right((json \ "id").as[String])
        case None => Left(ArangoError(500, 0, "Empty response"))
      }
    }
    requestExecutor.execute[String](
      method = "DELETE",
      url = s"collection/$name",
      handlers = List((200,  successHandler))
    )
  }

  override def truncate(name: String): Future[Either[ArangoError, Collection]] = {
    requestExecutor.execute[Collection](method = "PUT", url = s"collection/$name/truncate")
  }

  override def create(collectionProperties: CollectionCreateProperties): Future[Either[ArangoError, Collection]] = {
    requestExecutor.executeWithBody[Collection, CollectionCreateProperties](
      url = s"collection",
      body = collectionProperties
    )
  }

  override def load(name: String): Future[Either[ArangoError, Collection]] = {
    requestExecutor.execute[Collection](method = "PUT", url = s"collection/$name/load")
  }

  override def unload(name: String): Future[Either[ArangoError, Collection]] = {
    requestExecutor.execute[Collection](method = "PUT", url = s"collection/$name/unload")
  }

  override def rename(name: String, newName: String): Future[Either[ArangoError, Collection]] = {
    requestExecutor.executeWithBody[Collection, JsObject](
      method = "PUT",
      url = s"collection/$name/rename",
      body = Json.obj("name" -> newName)
    )
  }


  override def rotate(name: String): Future[Either[ArangoError, Boolean]] = {
    def successHandler(body: Option[JsValue], headers: Map[String, Seq[String]]) = {
      body match {
        case Some(json) => Right((json \ "result").as[Boolean])
        case None => Left(ArangoError(500, 0, "Empty response"))
      }
    }
    def noJournalHandler(body: Option[JsValue], headers: Map[String, Seq[String]]) = {
      body match {
        case Some(json) => Right(!(json \ "errorNum").asOpt[Int].contains(1105))
        case None => Left(ArangoError(500, 0, "Empty response"))
      }
    }
    requestExecutor.execute[Boolean](
      method = "PUT",
      url = s"collection/$name/rotate",
      handlers = List(
        (200, successHandler),
        (400, noJournalHandler)
      )
    )
  }

  override def setWaitForSync(name: String, waitForSync: Boolean): Future[Either[ArangoError, CollectionProperties]] = {
    requestExecutor.executeWithBody[CollectionProperties, JsObject](
      method = "PUT",
      url = s"collection/$name/properties",
      body = Json.obj("waitForSync" -> waitForSync)
    )
  }

  override def setJournalSize(name: String, journalSize: Long): Future[Either[ArangoError, CollectionProperties]] = {
    requestExecutor.executeWithBody[CollectionProperties, JsObject](
      method = "PUT",
      url = s"collection/$name/properties",
      body = Json.obj("journalSize" -> journalSize)
    )
  }
}
