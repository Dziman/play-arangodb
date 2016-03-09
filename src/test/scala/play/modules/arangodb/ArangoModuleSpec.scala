package play.modules.arangodb

import com.google.inject
import org.specs2.mutable.Specification
import play.api.inject.guice.GuiceApplicationBuilder

object ArangoModuleSpec extends Specification {
  "Play integration" title

  "Arango API" should {
    "not be resolved if the module is not enabled" in {
      val application = new GuiceApplicationBuilder().build

      application.injector.instanceOf[ArangoApi].
        aka("Arango API") must throwA[inject.ConfigurationException]
    }

    "not be resolved if mandatory settings not set" in {
      applicationWithModule.injector.instanceOf[ArangoApi].
        aka("Arango API") must throwA[com.google.inject.ProvisionException]
    }

    "be resolved if the module is enabled" in {
      validApplication.injector.instanceOf[ArangoApi].
          aka("Arango API") must beAnInstanceOf[DefaultArangoApi]
    }

    "not be resolved if user is set without password" in {
      applicationWithUserOnly.injector.instanceOf[ArangoApi].
        aka("Arango API") must throwA[RuntimeException]
    }

    "be resolved if auth is set properly" in {
      applicationWithAuth.injector.instanceOf[ArangoApi].
        aka("Arango API") must beAnInstanceOf[DefaultArangoApi]
    }
  }

  def applicationWithModule = {
    import scala.collection.JavaConversions.iterableAsScalaIterable

    val env = play.api.Environment.simple(mode = play.api.Mode.Test)
    val config = play.api.Configuration.load(env)
    val modules = config.getStringList("play.modules.enabled").
      fold(List.empty[String])(l => iterableAsScalaIterable(l).toList)

    new GuiceApplicationBuilder().configure("play.modules.enabled" -> (modules :+ "play.modules.arangodb.ArangoModule"))

  }

  def validApplication = applicationWithModule.configure("play.arangodb.db" -> "mydb")

  def applicationWithUserOnly = validApplication.configure("play.arangodb.user" -> "someUser")

  def applicationWithAuth = applicationWithUserOnly.configure("play.arangodb.password" -> "secret")
}
