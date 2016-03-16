package play.modules.arangodb

import play.modules.arangodb.model.CollectionCreateProperties

object CollectionsApiSpec extends PlayArangoSpec {

  lazy val Collection = validApplication.injector.instanceOf[ArangoApi].Collection

  s"Collections" title

  private val notExistingCollectionName = "not-existing-collection"

  private val collectionForTest = "test-collection"

  private val emptyCollection = "empty-collection"

  "Arango API" should {
    "return none for info with not existing collection" in new WithFreshDb {
      await(Collection.info(name = notExistingCollectionName)) must be empty
    }

    "return collection info" in new WithFreshDb {
      val collectionInfoResult = await(Collection.info(name = collectionForTest))
      collectionInfoResult must be some
      val collectionInfo = collectionInfoResult.get
      collectionInfo.name must be_==(collectionForTest)
      collectionInfo.isSystem must be_==(false)
    }

    "return none if call count for not existing collection" in new WithFreshDb {
      await(Collection.documentsCount(name = notExistingCollectionName)) must be empty
    }

    "count documents number in collection" in new WithFreshDb {
      await(Collection.documentsCount(name = collectionForTest)) getOrElse 0L === 2L
    }

    "return 0 as documents number for empty collection" in new WithFreshDb {
      await(Collection.documentsCount(name = emptyCollection)) getOrElse -1L === 0L
    }

    "return none for properties with not existing collection" in new WithFreshDb {
      await(Collection.properties(name = notExistingCollectionName)) must be empty
    }

    "return collection properties" in new WithFreshDb {
      val collectionPropertiesResult = await(Collection.properties(name = collectionForTest))
      collectionPropertiesResult must be some
      val collectionProperties = collectionPropertiesResult.get
      collectionProperties.name must be_==(collectionForTest)
      collectionProperties.figures must be empty
    }

    "return none for statistics with not existing collection" in new WithFreshDb {
      await(Collection.statistics(name = notExistingCollectionName)) must be empty
    }

    "return collection statistics" in new WithFreshDb {
      val collectionStatisticsResult = await(Collection.statistics(name = collectionForTest))
      collectionStatisticsResult must be some
      val collectionStatistics = collectionStatisticsResult.get
      collectionStatistics.name must be_==(collectionForTest)
      collectionStatistics.figures must be some
    }

    "return none for revision with not existing collection" in new WithFreshDb {
      await(Collection.revision(name = notExistingCollectionName)) must be empty
    }

    "return collection revision" in new WithFreshDb {
      val collectionRevisionResult = await(Collection.revision(name = collectionForTest))
      collectionRevisionResult must be some
      val collectionRevision = collectionRevisionResult.get
      collectionRevision.name must be_==(collectionForTest)
      collectionRevision.revision must be some
    }

    "return none for checksum with not existing collection" in new WithFreshDb {
      await(Collection.checksum(name = notExistingCollectionName)) must be empty
    }

    "return collection checksum" in new WithFreshDb {
      val ch1Result = await(Collection.checksum(name = collectionForTest))
      ch1Result must be some
      val ch1 = ch1Result.get.checksum
      ch1 must be some
      val ch2Result = await(Collection.checksum(name = collectionForTest, withRevisions = true))
      ch2Result must be some
      val ch2 = ch2Result.get.checksum
      ch2 must be some
      val ch3Result = await(Collection.checksum(name = collectionForTest, withData = true))
      ch3Result must be some
      val ch3 = ch3Result.get.checksum
      ch3 must be some
      val ch4Result = await(Collection.checksum(name = collectionForTest, withRevisions = true, withData = true))
      ch4Result must be some
      val ch4 = ch4Result.get.checksum
      ch4 must be some
      val ch5Result = await(Collection.checksum(name = collectionForTest, withRevisions = false, withData = false))
      ch5Result must be some
      val ch5 = ch5Result.get.checksum
      ch5 must be some

      ch1 === Some(172416944L)
      ch3 === Some(2742834720L)
      ch5 === Some(172416944L)
      // can't check checksums calculated withRevisions = true due to every db restore changes revision

      ch1 must be_!=(ch2)
      ch1 must be_!=(ch3)
      ch1 must be_!=(ch4)
      ch2 must be_!=(ch3)
      ch2 must be_!=(ch4)
      ch3 must be_!=(ch4)
      ch1 must be_==(ch5)
    }

    "return none if try to call not existing DB" in {
      val Collection = validApplication.configure("play.arangodb.db" -> "play-arangodb-tests-not-existing").injector.instanceOf[ArangoApi].Collection
      await(Collection.all()) must be empty
    }

    "show non-system collections" in new WithFreshDb {
      val collectionsResult = await(Collection.all())
      collectionsResult must be some
      val collections = collectionsResult.get
      collections.collections must have size 2
      val collectionsExplicitSystemResult = await(Collection.all(withSystem = false))
      collectionsExplicitSystemResult must be some
      val collectionsExplicitSystem = collectionsExplicitSystemResult.get
      collectionsExplicitSystem must be_==(collections)
    }

    "show collections(including system)" in new WithFreshDb {
      val collectionsResult = await(Collection.all(withSystem = true))
      collectionsResult must be some
      val collections = collectionsResult.get
      collections.collections must have size 14
    }

    "do nothing if try to delete not existing collection" in new WithFreshDb {
      val collectionsBefore = await(Collection.all(withSystem = true))
      await(Collection.drop(name = notExistingCollectionName)) must be empty
      val collectionsAfter = await(Collection.all(withSystem = true))
      collectionsBefore === collectionsAfter
    }

    "delete proper collection" in new WithFreshDb {
      val collectionInfo = await(Collection.info(name = collectionForTest))
      val deleteResult = await(Collection.drop(name = collectionForTest))
      deleteResult must be some

      deleteResult.get === collectionInfo.get.id

      await(Collection.info(name = collectionForTest)) must be empty
    }

    "return none if try to truncate not existing collection" in new WithFreshDb {
      await(Collection.truncate(name = notExistingCollectionName)) must be empty
    }

    "truncate collection" in new WithFreshDb {
      val collectionInfo = await(Collection.info(name = collectionForTest))
      val truncateResult = await(Collection.truncate(name = collectionForTest))
      truncateResult must be some

      truncateResult.get.id === collectionInfo.get.id

      await(Collection.documentsCount(name = collectionForTest)) === Some(0L)
    }

    "create new collection" in new WithFreshDb {
      await(Collection.create(CollectionCreateProperties(name = notExistingCollectionName))) must be some

      await(Collection.info(name = notExistingCollectionName)) must be some
    }

    "create new system collection with given properties" in new WithFreshDb {
      val collectionCreateProperties = CollectionCreateProperties(
        name = s"_$notExistingCollectionName",
        isSystem = Some(true),
        indexBuckets = Some(32),
        doCompact = Some(false)
      )
      await(Collection.create(collectionCreateProperties)) must be some

      val createdCollectionPropertiesResult = await(Collection.properties(name = s"_$notExistingCollectionName"))
      createdCollectionPropertiesResult must be some

      val createdCollectionProperties = createdCollectionPropertiesResult.get

      createdCollectionProperties.isSystem === true
      createdCollectionProperties.indexBuckets === 32
      createdCollectionProperties.doCompact === false

    }

    "create new collection with given properties" in new WithFreshDb {
      val collectionCreateProperties = CollectionCreateProperties(
        name = notExistingCollectionName,
        isSystem = Some(false),
        indexBuckets = Some(13),
        doCompact = Some(true),
        journalSize = Some(1048576L)
      )
      await(Collection.create(collectionCreateProperties)) must be some

      val createdCollectionPropertiesResult = await(Collection.properties(name = notExistingCollectionName))
      createdCollectionPropertiesResult must be some

      val createdCollectionProperties = createdCollectionPropertiesResult.get

      createdCollectionProperties.isSystem === false
      createdCollectionProperties.indexBuckets === 13
      createdCollectionProperties.doCompact === true

    }

    "fail to create new collection with existing name" in new WithFreshDb {
      await(Collection.create(CollectionCreateProperties(name = collectionForTest))) must throwA(ArangoException(409, "Unexpected response status code"))
    }

    "fail to create new collection with invalid properties" in new WithFreshDb {
      await(Collection.create(CollectionCreateProperties(name = notExistingCollectionName, journalSize = Some(666)))) must throwA(ArangoException(400, "Unexpected response status code"))
    }

    "return none if try to load not existing collection" in new WithFreshDb {
      await(Collection.load(name = notExistingCollectionName)) must be empty
    }

    "load collection" in new WithFreshDb {
      val loadResult = await(Collection.load(name = collectionForTest))
      loadResult must be some

      loadResult.get.status === 3
    }

    "return none if try to unload not existing collection" in new WithFreshDb {
      await(Collection.unload(name = notExistingCollectionName)) must be empty
    }

    "unload collection" in new WithFreshDb {
      val unloadResult = await(Collection.unload(name = collectionForTest))
      unloadResult must be some

      unloadResult.get.status === 2 or unloadResult.get.status === 4
    }

    "return none if try to rename not existing collection" in new WithFreshDb {
      await(Collection.rename(name = notExistingCollectionName, newName = s"$notExistingCollectionName-renamed")) must be empty
    }

    "not rename collection to if new name used by another existing collection" in new WithFreshDb {
      await(Collection.rename(name = collectionForTest, newName = emptyCollection)) must be empty
    }

    "rename collection" in new WithFreshDb {
      val beforeRenameInfo = await(Collection.info(name = collectionForTest)).get
      val renameResult = await(Collection.rename(name = collectionForTest, newName = notExistingCollectionName))
      renameResult must be some

      renameResult.get.id === beforeRenameInfo.id

      await(Collection.info(name = collectionForTest)) must be empty
    }

    "return none if try to rotate journal for not existing collection" in new WithFreshDb {
      await(Collection.rotate(name = notExistingCollectionName)) must be empty
    }

    "not rotate not existing collection journal" in new WithFreshDb {
      val rotateResult = await(Collection.rotate(name = collectionForTest))
      rotateResult must be some

      rotateResult.get === false
    }

    "return none if try to set waitForSync for not existing collection" in new WithFreshDb {
      await(Collection.setWaitForSync(name = notExistingCollectionName, waitForSync = false)) must be empty
    }

    "update waitForSync property" in new WithFreshDb {
      val setWaitForSyncResult = await(Collection.setWaitForSync(name = collectionForTest, waitForSync = true))
      setWaitForSyncResult must be some

      setWaitForSyncResult.get.waitForSync === true
    }

    "return none if try to set journalSize for not existing collection" in new WithFreshDb {
      await(Collection.setJournalSize(name = notExistingCollectionName, journalSize = 1048576L)) must be empty
    }

    "fail to update collection's properties with invalid journal size" in new WithFreshDb {
      await(Collection.setJournalSize(name = collectionForTest, journalSize = 666)) must throwA(ArangoException(400, "Unexpected response status code"))
    }

    "update journal size" in new WithFreshDb {
      private val newJournalSize = 1048576L
      val setJournalSizeResult = await(Collection.setJournalSize(name = collectionForTest, journalSize = newJournalSize))
      setJournalSizeResult must be some

      setJournalSizeResult.get.journalSize === newJournalSize
    }
  }
}
