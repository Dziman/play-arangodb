package play.modules
package arangodb

import javax.inject.{Inject, Provider, Singleton}

import play.api.inject.{Binding, Module}
import play.api.{Configuration, Environment, PlayConfig}


@Singleton
final class ArangoModule extends Module {
  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] =
    Seq(
      bind[ArangoConfiguration].toProvider[ArangoConfigurationProvider],
      bind[RequestExecutor].to[DefaultRequestExecutor].in[Singleton],
      bind[CollectionApi].to[RestCollectionApi].in[Singleton],
      bind[DocumentApi].to[RestDocumentApi].in[Singleton]
    )
}

class ArangoConfigurationProvider @Inject()(configuration: Configuration) extends Provider[ArangoConfiguration] {
  override def get() = ArangoConfiguration(PlayConfig(configuration).get[PlayConfig]("play.arangodb"))
}

