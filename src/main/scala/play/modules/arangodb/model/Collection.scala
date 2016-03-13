package play.modules.arangodb.model

import org.joda.time.DateTime

case class Collection(
                       id: String,
                       name: String,
                       isSystem: Boolean,
                       status: Int,
                       `type`: Int,
                       checksum: Option[Long],
                       revision: Option[String])

case class CollectionProperties(
                                 id: String,
                                 name: String,
                                 isSystem: Boolean,
                                 status: Int,
                                 `type`: Int,
                                 count: Option[Long],
                                 indexBuckets: Int,
                                 waitForSync: Boolean,
                                 doCompact: Boolean,
                                 journalSize: Long,
                                 isVolatile: Boolean,
                                 numberOfShards: Option[Int],
                                 shardKeys: Option[List[String]],
                                 figures: Option[Figures])

case class KeyOptions(`type`: String, allowUserKeys: Boolean)

case class Figures(
                    alive: Figure,
                    dead: Figure,
                    datafiles: Figure,
                    journals: Figure,
                    compactors: Figure,
                    shapefiles: Figure,
                    shapes: Figure,
                    attributes: Figure,
                    indexes: Figure,
                    lastTick: String,
                    uncollectedLogfileEntries: Int,
                    documentReferences: Long,
                    waitingFor: String,
                    compactionStatus: CompactionStatus
                  )

case class Figure(
                   count: Long,
                   size: Option[Long],
                   fileSize: Option[Long],
                   deletion: Option[Long]
                 )

case class Collections(collections: Seq[Collection], names: Map[String, Collection])

case class CompactionStatus(message: String, time: DateTime)
