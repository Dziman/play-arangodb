package play.modules.arangodb

import com.google.inject
import org.specs2.mutable.Specification
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeApplication
import play.api.test.Helpers._


object ArangoModuleSpec extends Specification {
  "Play integration" title

  "Arango API" should {
    "not be resolved if the module is not enabled" in running(FakeApplication()) {
      val appBuilder = new GuiceApplicationBuilder().build

      appBuilder.injector.instanceOf[ArangoApi].
        aka("Arango API") must throwA[inject.ConfigurationException]
    }

    "be resolved if the module is enabled" in running(FakeApplication()) {
        configuredAppBuilder.injector.instanceOf[ArangoApi].
          aka("Arango API") must beAnInstanceOf[DefaultArangoApi]
      }
  }

  def configuredAppBuilder = {
    import scala.collection.JavaConversions.iterableAsScalaIterable

    val env = play.api.Environment.simple(mode = play.api.Mode.Test)
    val config = play.api.Configuration.load(env)
    val modules = config.getStringList("play.modules.enabled").
      fold(List.empty[String])(l => iterableAsScalaIterable(l).toList)

    new GuiceApplicationBuilder().
      configure("play.modules.enabled" -> (modules :+ "play.modules.arangodb.ArangoModule")).build
  }
}
