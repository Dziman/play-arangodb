package play.modules.arangodb

import javax.inject.Inject

import play.modules.arangodb.model._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


trait CollectionApi {
  def info(name: String): Future[Option[Collection]]

  def properties(name: String): Future[Option[CollectionProperties]]

  def documentsCount(name: String): Future[Option[Long]]

  def statistics(name: String): Future[Option[CollectionProperties]]

  def revision(name: String): Future[Option[Collection]]

  def checksum(name: String, withRevisions: Boolean = false, withData: Boolean = false): Future[Option[Collection]]

  def all(withSystem: Boolean = false): Future[Option[Collections]]
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
}
