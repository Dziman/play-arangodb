package play.modules.arangodb

import java.net.ConnectException
import javax.inject.Inject

import play.api.libs.json._
import play.api.libs.ws.WSAuthScheme.BASIC
import play.api.libs.ws.{EmptyBody, WSBody, WSClient}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait RequestExecutor {
  private[play] def constructBaseUrl: String

  private[play] def execute[T](
                                method: String = "GET",
                                url: String,
                                body: WSBody = EmptyBody,
                                query: Seq[(String, String)] = Seq.empty,
                                headers: Seq[(String, String)] = Seq.empty,
                                handlers: Seq[(Int, Option[JsValue] => Option[T])] = Seq.empty
                              )(implicit objectReads: Reads[T]): Future[Option[T]]
}

class DefaultRequestExecutor @Inject()(conf: ArangoConfiguration, ws: WSClient) extends RequestExecutor {
  lazy protected val baseUrl = constructBaseUrl

  override private[play] def constructBaseUrl: String = {
    val protocol = if (conf.ssl) "https" else "http"
    s"$protocol://${conf.host}:${conf.port}/_db/${conf.db}/_api"
  }

  override private[play] def execute[T](
                                         method: String = "GET",
                                         url: String,
                                         body: WSBody = EmptyBody,
                                         query: Seq[(String, String)] = Seq.empty,
                                         headers: Seq[(String, String)] = Seq.empty,
                                         handlers: Seq[(Int, Option[JsValue] => Option[T])] = Seq.empty
                                       )(implicit objectReads: Reads[T]): Future[Option[T]] = {
    try {
      // TODO Extract timeout to config
      // TODO Add proxy support?
      var request = ws.url(s"$baseUrl/$url").
        withMethod(method).
        withRequestTimeout(15 * 1000).
        withFollowRedirects(true).
        withQueryString(query: _*).
        withHeaders(headers: _*).
        withBody(body)
      request = conf.user.fold(request) { userName => request.withAuth(userName, conf.password.get, BASIC) }

      request.execute() map { response =>
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
            val defaultHandlers: Seq[(Int, Option[JsValue] => Option[T])] = List(
              (200, body => body.map { json => parseResponseJson[T](json) }),
              (404, _ => None)
            )

            val status = response.status
            // successful response or response with not general error
            val suitableHandlers = (handlers ++ defaultHandlers) filter { handlerForStatus =>
              val (statusWithHandler, _) = handlerForStatus
              statusWithHandler == status
            }
            // TODO Try to get standard ArangoDB response fields (error, errorNumber etc.) and process according this info
            if (suitableHandlers.isEmpty) throw new ArangoException(601, s"Unexpected response status code $status")

            val json = if (response.body.nonEmpty) Some(response.json) else None
            // more than one handler is meaningless so exec the first one and ignore other
            suitableHandlers.head._2(json)
        }
      }
    } catch {
      case _: ConnectException => throw new ArangoException(408, "No response from ArangoDB server.")
    }
  }

  private def parseResponseJson[T](json: JsValue)(implicit objectReads: Reads[T]): T = {
    json.validate[T] fold(
      error => throw new ArangoException(600, "Can't parse response"),
      responseObject => responseObject
      )
  }
}
