package play.modules.arangodb

import com.google.inject
import org.specs2.mutable.Specification
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.bind

object ArangoModuleSpec extends Specification {
  "Play integration" title

  "Arango API" should {
    "not be resolved if the module is not enabled" in {
      val application = new GuiceApplicationBuilder().build

      application.injector.instanceOf[ArangoApi].
        aka("Arango API") must throwA[inject.ConfigurationException]
    }

    "not be resolved if mandatory settings not set" in {
      applicationBuilderWithModule.build.injector.instanceOf[ArangoApi].
        aka("Arango API") must throwA[com.google.inject.ProvisionException]
    }

    "be resolved if the module is enabled" in {
      validApplication.injector.instanceOf[ArangoApi].
          aka("Arango API") must beAnInstanceOf[DefaultArangoApi]
    }

  }

  def applicationBuilderWithModule = {
    import scala.collection.JavaConversions.iterableAsScalaIterable

    val env = play.api.Environment.simple(mode = play.api.Mode.Test)
    val config = play.api.Configuration.load(env)
    val modules = config.getStringList("play.modules.enabled").
      fold(List.empty[String])(l => iterableAsScalaIterable(l).toList)

    new GuiceApplicationBuilder().configure("play.modules.enabled" -> (modules :+ "play.modules.arangodb.ArangoModule"))

  }

  def validApplication = applicationBuilderWithModule.configure("play.arangodb.db" -> "db").build
}
