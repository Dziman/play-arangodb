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
  def apply(config: PlayConfig) = new ArangoConfiguration(
    config.get[String]("host"),
    config.get[Int]("port"),
    config.get[Boolean]("ssl"),
    config.getOptional[String]("user"),
    config.getOptional[String]("password"),
    config.get[String]("db")
  )
}

class ArangoConfigurationProvider @Inject()(configuration: Configuration) extends Provider[ArangoConfiguration] {
  override def get() = ArangoConfiguration(PlayConfig(configuration).get[PlayConfig]("play.arangodb"))
}

class ArangoConfigurationModule extends Module {
  override def bindings(environment: Environment, configuration: Configuration) = Seq(
    bind[ArangoConfiguration].toProvider[ArangoConfigurationProvider]
  )
}
