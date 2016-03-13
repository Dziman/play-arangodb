package play.modules.arangodb

import com.google.inject
import play.api.inject.guice.GuiceApplicationBuilder

object ArangoModuleSpec extends PlayArangoSpec {
  s"Play integration module" title

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

  def applicationWithUserOnly = validApplication.configure("play.arangodb.user" -> "someUser")

  def applicationWithAuth = applicationWithUserOnly.configure("play.arangodb.password" -> "secret")
}
