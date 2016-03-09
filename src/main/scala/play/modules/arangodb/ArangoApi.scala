package play.modules.arangodb

import javax.inject.{Inject, Provider}

import play.api.inject.Module
import play.api.{Configuration, Environment, PlayConfig}


trait ArangoApi {}

final class DefaultArangoApi @Inject() (configuration: ArangoConfiguration) extends ArangoApi {}

case class ArangoConfiguration(
                                host: String,
                                port: Int,
                                ssl: Boolean,
                                user: Option[String],
                                password: Option[String],
                                db: String
                              )

object ArangoConfiguration {
  def apply(config: PlayConfig) = {
    val user = config.getOptional[String]("user")
    val password = config.getOptional[String]("password")
    if (user.isDefined && password.isEmpty) throw new RuntimeException("[ArangoDB module] Password is required if user is set")
    new ArangoConfiguration(
      config.get[String]("host"),
      config.get[Int]("port"),
      config.get[Boolean]("ssl"),
      user,
      password,
      config.get[String]("db")
    )
  }
}

class ArangoConfigurationProvider @Inject()(configuration: Configuration) extends Provider[ArangoConfiguration] {
  override def get() = ArangoConfiguration(PlayConfig(configuration).get[PlayConfig]("play.arangodb"))
}

class ArangoConfigurationModule extends Module {
  override def bindings(environment: Environment, configuration: Configuration) = Seq(
    bind[ArangoConfiguration].toProvider[ArangoConfigurationProvider]
  )
}
