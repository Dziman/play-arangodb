package play.modules
package arangodb

import java.net.ConnectException
import javax.inject.Inject

import play.api.libs.json._
import play.api.libs.ws.WSAuthScheme.BASIC
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import play.modules.arangodb.model._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait RequestExecutor {
  private[play] def constructBaseUrl: String

  private[play] def execute[T](
                                method: String = "GET",
                                url: String,
                                query: Seq[(String, String)] = Seq.empty,
                                headers: Seq[(String, String)] = Seq.empty,
                                handlers: Seq[(Int, (Option[JsValue], Map[String, Seq[String]]) => Either[ArangoError, T])] = Seq.empty
                              )(implicit objectReads: Reads[T]): Future[Either[ArangoError, T]]

  private[play] def executeWithBody[T, B](
                                           method: String = "POST",
                                           url: String,
                                           body: B,
                                           query: Seq[(String, String)] = Seq.empty,
                                           headers: Seq[(String, String)] = Seq.empty,
                                           handlers: Seq[(Int, (Option[JsValue], Map[String, Seq[String]]) => Either[ArangoError, T])] = Seq.empty
                                         )(implicit objectReads: Reads[T], bodyWrites: Writes[B]): Future[Either[ArangoError, T]]

  private[play] def defaultSuccessHandler[T](
                                              body: Option[JsValue],
                                              headers: Map[String, Seq[String]]
                                            )(implicit objectReads: Reads[T]): Either[ArangoError, T]
}

// TODO Do not use shared WSClient and create our own?
class DefaultRequestExecutor @Inject()(conf: ArangoConfiguration, ws: WSClient) extends RequestExecutor {
  lazy protected val baseUrl = constructBaseUrl

  override private[play] def constructBaseUrl: String = {
    val protocol = if (conf.ssl) "https" else "http"
    s"$protocol://${conf.host}:${conf.port}/_db/${conf.db}/_api"
  }

  override private[play] def execute[T](
                                         method: String = "GET",
                                         url: String,
                                         query: Seq[(String, String)] = Seq.empty,
                                         headers: Seq[(String, String)] = Seq.empty,
                                         handlers: Seq[(Int, (Option[JsValue], Map[String, Seq[String]]) => Either[ArangoError, T])] = Seq.empty
                                       )(implicit objectReads: Reads[T]): Future[Either[ArangoError, T]] = {
    val request = prepareRequest(method, url, query, headers)
    executeInternally(request, handlers)
  }

  override private[play] def executeWithBody[T, B](
                                                    method: String = "POST",
                                                    url: String,
                                                    body: B,
                                                    query: Seq[(String, String)] = Seq.empty,
                                                    headers: Seq[(String, String)] = Seq.empty,
                                                    handlers: Seq[(Int, (Option[JsValue], Map[String, Seq[String]]) => Either[ArangoError, T])] = Seq.empty
                                                  )(implicit objectReads: Reads[T], bodyWrites: Writes[B]): Future[Either[ArangoError, T]] = {
    val request = prepareRequest(method, url, query, headers).withBody(Json.toJson(body))
    executeInternally(request, handlers)
  }

  private def prepareRequest(
                              method: String,
                              url: String,
                              query: Seq[(String, String)],
                              headers: Seq[(String, String)]) = {
    // TODO Add proxy support?
    val request = ws.url(s"$baseUrl/$url").
      withMethod(method).
      withRequestTimeout(conf.timeout * 1000).
      withFollowRedirects(true).
      withQueryString(query: _*).
      withHeaders(headers: _*)

    conf.user.fold(request) { userName => request.withAuth(userName, conf.password.get, BASIC) }
  }

  private def executeInternally[T](
                                    request: WSRequest,
                                    handlers: Seq[(Int, (Option[JsValue], Map[String, Seq[String]]) => Either[ArangoError, T])]
                                  )(implicit objectReads: Reads[T]): Future[Either[ArangoError, T]] = {
    try {
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
          => defaultErrorHandler(response)
          case status if true || status >= 200 && status <= 299 =>
            val defaultHandlers: Seq[(Int, (Option[JsValue], Map[String, Seq[String]]) => Either[ArangoError, T])] = List(
              (200, defaultSuccessHandler[T])
            )

            val status = response.status
            // successful response or response with not general error
            val suitableHandlers = (handlers ++ defaultHandlers) filter { handlerForStatus =>
              val (statusWithHandler, _) = handlerForStatus
              statusWithHandler == status
            }

            if (suitableHandlers.par.nonEmpty) {
              val json = if (response.body.nonEmpty) Some(response.json) else None
              // more than one handler is meaningless so exec the first one and ignore other
              suitableHandlers.head._2(json, response.allHeaders)
            } else {
              defaultErrorHandler(response)
            }
        }
      }
    } catch {
      case _: ConnectException => Future.successful(Left(ArangoError(408, 0, "No response from ArangoDB server.")))
    }
  }

  override def defaultSuccessHandler[T](
                                         body: Option[JsValue],
                                         headers: Map[String, Seq[String]]
                                       )(implicit objectReads: Reads[T]): Either[ArangoError, T] =
    body match {
      case Some(json) => parseResponseJson[T](json)
      case None => Left(ArangoError(500, 0, "Empty response"))
    }

  private def parseResponseJson[T](json: JsValue)(implicit objectReads: Reads[T]): Either[ArangoError, T] = {
    json.validate[T] fold(
      error => Left(ArangoError(600, 0, "Can't parse response")),
      responseObject => Right(responseObject)
      )
  }

  private def defaultErrorHandler(response: WSResponse) = {
    if (response.body.nonEmpty) {
      response.json.validate[ArangoError] fold(
        error => Left(ArangoError(response.status, 0, s"Unexpected response status code")),
        responseError => Left(responseError)
        )
    } else {
      Left(ArangoError(response.status, 0, s"Unexpected response status code"))
    }
  }
}
