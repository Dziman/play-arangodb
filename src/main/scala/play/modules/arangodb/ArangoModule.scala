package play.modules.arangodb

import javax.inject.Singleton
import play.api.{ Configuration, Environment }
import play.api.inject.{ Binding, Module }


@Singleton
final class ArangoModule extends Module {
  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = Seq(bind[ArangoApi].to[DefaultArangoApi].in[Singleton])
}
