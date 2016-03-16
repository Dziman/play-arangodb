package play.modules
package arangodb

/**
  * Exception thrown when some problem occurred with ArangoDB communication.
  *
  * @param statusCode HTTP status code returned by ArangoDB API or calculated from exception thrown (408 for TimeoutException for example) // TODO Document custom (6xx codes)
  * @param message Detailed description
  */
case class ArangoException(statusCode: Int, message: String) extends RuntimeException(message) {
  // TODO Better message construction
  override def getMessage: String = s"$statusCode ${super.getMessage}"
}
