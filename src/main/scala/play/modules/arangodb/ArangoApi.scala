package play.modules
package arangodb

import javax.inject.Inject

import play.api.PlayConfig

case class ArangoApi @Inject()(Collection: CollectionApi)

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
