package play.modules.arangodb

import javax.inject.Inject
import play.api.Configuration


trait ArangoApi {}

final class DefaultArangoApi @Inject() (configuration: Configuration) extends ArangoApi {}
