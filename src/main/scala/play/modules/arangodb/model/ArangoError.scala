package play.modules.arangodb.model

case class ArangoError(code: Int,
                       errorNum: Int,
                       errorMessage: String,
                       _id: Option[String] = None,
                       _key: Option[String] = None,
                       _rev: Option[String] = None
                      )
