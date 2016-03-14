package play.modules.arangodb

import org.specs2.mutable.BeforeAfter
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

trait WithFreshDb extends BeforeAfter {
  override def before = {
    import org.specs2.control.Executable._

    import scala.sys.process._
    "arangorestore --create-collection true --create-database true --import-data true --include-system-collections true --overwrite true --server.database play-arangodb-tests --input-directory src/test/resources/db".!(NullProcessLogger)
  }

  override def after = {
    import org.specs2.control.Executable._

    import scala.sys.process._
    "arangosh --javascript.execute-string db._dropDatabase('play-arangodb-tests')".!(NullProcessLogger)
  }
}
