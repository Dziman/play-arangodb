package play.modules.arangodb

import java.net.ConnectException
import javax.inject.{Inject, Provider}

import play.api.inject.Module
import play.api.libs.ws.WSAuthScheme.BASIC
import play.api.libs.ws.{WSClient, WSRequest}
import play.api.{Configuration, Environment, PlayConfig}

import scala.concurrent.{Await, TimeoutException}
import scala.concurrent.duration._

trait ArangoApi {}

final class DefaultArangoApi @Inject()(conf: ArangoConfiguration, ws: WSClient) extends ArangoApi {
  lazy val baseUrl = constructBaseUrl

  private def prepareRequest(requestUrl: String, method: String = "GET"): WSRequest = {
    // TODO Add proxy support?
    // TODO Extract timeout to config
    val request = ws.url(s"$baseUrl/$requestUrl").withMethod(method).withRequestTimeout(15 * 1000).withFollowRedirects(true)
    conf.user map { userName => request.withAuth(userName, conf.password.get, BASIC) } getOrElse request
  }

  private def constructBaseUrl: String = {
    val protocol = if (conf.ssl) "https" else "http"
    s"$protocol://${conf.host}:${conf.port}/_db/${conf.db}/_api"
  }

  private def execute(request: WSRequest) = {
    try {
      val response = Await.result(request.execute(), 15 seconds)

      //noinspection SimplifyBoolean
      response.status match {
        /**
          * Handle general purpose errors from ArangoDB API:
          * 401 (Unauthorized)
          * 405 (Method Not Allowed) HTTP method not allowed for provided url
          * 413 (Request Entity Too Large) when clients send a body or a Content-Length value bigger than the maximum allowed value (512 MB)
          * 414 (Request-URI too long) the maximum URL length accepted by ArangoDB is 16K
          * 431 (Request Header Fields Too Large) if the overall length of the HTTP headers a client sends for one request exceeds the maximum allowed size (1 MB)
          * 500 (Internal error)
          *
          * @see <a href="https://docs.arangodb.com/GeneralHttp/index.html">General HTTP Request Handling in ArangoDB</a>
          */
        case status
          if
          status == 401 ||
            status == 405 ||
            status == 413 || status == 414 || status == 431 ||
            status == 500
        => throw new ArangoException(status, response.statusText)
        case status if true || status >= 200 && status <= 299 =>
          // successful response or response with not general error
          // TODO Try to get standard ArangoDB response fields (error, errorNumber etc.) and process according this info
          response
      }
    } catch {
      case _@(_: ConnectException | _: TimeoutException) => throw new ArangoException(408, "No response from ArangoDB server.")
    }
  }
}

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
