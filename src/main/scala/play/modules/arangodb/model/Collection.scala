package play.modules
package arangodb.model

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
                                 keyOptions: Option[KeyOptions],
                                 numberOfShards: Option[Int],
                                 shardKeys: Option[List[String]],
                                 figures: Option[Figures])

case class KeyOptions(
                       `type`: String,
                       allowUserKeys: Boolean,
                       increment: Option[Int],
                       offset: Option[Long]
                     )

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

case class CompactionStatus(message: String, time: DateTime)

case class Collections(collections: Seq[Collection], names: Map[String, Collection])

case class CollectionCreateProperties(
                                       name: String,
                                       journalSize: Option[Long] = None,
                                       keyOptions: Option[KeyOptions] = None,
                                       waitForSync: Option[Boolean] = None,
                                       doCompact: Option[Boolean] = None,
                                       isVolatile: Option[Boolean] = None,
                                       shardKeys: Option[List[String]] = None,
                                       numberOfShards: Option[Int] = None,
                                       isSystem: Option[Boolean] = None,
                                       `type`: Option[Int] = None,
                                       indexBuckets: Option[Int] = None
                                     )
