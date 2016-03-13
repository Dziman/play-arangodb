package play.modules.arangodb

import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.PlaySpecification

trait PlayArangoSpec extends PlaySpecification {

  def applicationWithModule = {
    import scala.collection.JavaConversions.iterableAsScalaIterable

    val env = play.api.Environment.simple(mode = play.api.Mode.Test)
    val config = play.api.Configuration.load(env)
    val modules = config.getStringList("play.modules.enabled").
      fold(List.empty[String])(l => iterableAsScalaIterable(l).toList)

    new GuiceApplicationBuilder().configure("play.modules.enabled" -> (modules :+ "play.modules.arangodb.ArangoModule"))

  }

  def validApplication = applicationWithModule.configure("play.arangodb.db" -> "play-arangodb-tests")

}
