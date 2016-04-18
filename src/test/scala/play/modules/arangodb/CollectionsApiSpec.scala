package play.modules.arangodb

import play.modules.arangodb.model.CollectionCreateProperties

object CollectionsApiSpec extends PlayArangoSpec {

  lazy val Collection = validApplication.injector.instanceOf[ArangoApi].Collection

  s"Collections" title

  private val notExistingCollectionName = "not-existing-collection"

  private val collectionForTest = "test-collection"

  private val emptyCollection = "empty-collection"

  "Arango API" should {
    "return error for info with not existing collection" in new WithFreshDb {
      val infoResult = await(Collection.info(name = notExistingCollectionName))
      infoResult must be left

      val error = infoResult.left.get
      error.code === 404
      error.errorNum === 1203
    }

    "return collection info" in new WithFreshDb {
      val collectionInfoResult = await(Collection.info(name = collectionForTest))
      collectionInfoResult must be right

      val collectionInfo = collectionInfoResult.right.get
      collectionInfo.name must be_==(collectionForTest)
      collectionInfo.isSystem must be_==(false)
    }

    "return error if call count for not existing collection" in new WithFreshDb {
      val countResult = await(Collection.documentsCount(name = notExistingCollectionName))
      countResult must be left

      val error = countResult.left.get
      error.code === 404
      error.errorNum === 1203
    }

    "count documents number in collection" in new WithFreshDb {
      await(Collection.documentsCount(name = collectionForTest)).right.get === 2L
    }

    "return 0 as documents number for empty collection" in new WithFreshDb {
      await(Collection.documentsCount(name = emptyCollection)).right.get === 0L
    }

    "return error for properties with not existing collection" in new WithFreshDb {
      val propertiesResult = await(Collection.properties(name = notExistingCollectionName))
      propertiesResult must be left

      val error = propertiesResult.left.get
      error.code === 404
      error.errorNum === 1203
    }


    "return collection properties" in new WithFreshDb {
      val collectionPropertiesResult = await(Collection.properties(name = collectionForTest))
      collectionPropertiesResult must be right

      val collectionProperties = collectionPropertiesResult.right.get
      collectionProperties.name === collectionForTest
      collectionProperties.figures must be empty
    }

    "return error for statistics with not existing collection" in new WithFreshDb {
      val statisticsResult = await(Collection.statistics(name = notExistingCollectionName))
      statisticsResult must be left

      val error = statisticsResult.left.get
      error.code === 404
      error.errorNum === 1203
    }

    "return collection statistics" in new WithFreshDb {
      val collectionStatisticsResult = await(Collection.statistics(name = collectionForTest))
      collectionStatisticsResult must be right

      val collectionStatistics = collectionStatisticsResult.right.get
      collectionStatistics.name must be_==(collectionForTest)
      collectionStatistics.figures must be some
    }

    "return error for revision with not existing collection" in new WithFreshDb {
      val revisionResult = await(Collection.revision(name = notExistingCollectionName))
      revisionResult must be left

      val error = revisionResult.left.get
      error.code === 404
      error.errorNum === 1203
    }

    "return collection revision" in new WithFreshDb {
      val collectionRevisionResult = await(Collection.revision(name = collectionForTest))
      collectionRevisionResult must be right

      val collectionRevision = collectionRevisionResult.right.get
      collectionRevision.name === collectionForTest
      collectionRevision.revision must be some
    }

    "return none for checksum with not existing collection" in new WithFreshDb {
      val checksumResult = await(Collection.checksum(name = notExistingCollectionName))
      checksumResult must be left

      val error = checksumResult.left.get
      error.code === 404
      error.errorNum === 1203
    }

    "return collection checksum" in new WithFreshDb {
      val ch1Result = await(Collection.checksum(name = collectionForTest))
      ch1Result must be right

      val ch1 = ch1Result.right.get.checksum
      ch1 must be some
      val ch2Result = await(Collection.checksum(name = collectionForTest, withRevisions = true))
      ch2Result must be right
      val ch2 = ch2Result.right.get.checksum
      ch2 must be some
      val ch3Result = await(Collection.checksum(name = collectionForTest, withData = true))
      ch3Result must be right
      val ch3 = ch3Result.right.get.checksum
      ch3 must be some
      val ch4Result = await(Collection.checksum(name = collectionForTest, withRevisions = true, withData = true))
      ch4Result must be right
      val ch4 = ch4Result.right.get.checksum
      ch4 must be some
      val ch5Result = await(Collection.checksum(name = collectionForTest, withRevisions = false, withData = false))
      ch5Result must be right
      val ch5 = ch5Result.right.get.checksum
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

    "return error if try to call not existing DB" in {
      val Collection = validApplication.configure("play.arangodb.db" -> "play-arangodb-tests-not-existing").injector.instanceOf[ArangoApi].Collection
      val allCollectionsResult = await(Collection.all())
      allCollectionsResult must be left

      val error = allCollectionsResult.left.get
      error.code === 404
      error.errorNum === 1228
    }

    "show non-system collections" in new WithFreshDb {
      val collectionsResult = await(Collection.all())
      collectionsResult must be right

      val collections = collectionsResult.right.get
      collections.collections must have size 2
      val collectionsExplicitSystemResult = await(Collection.all(withSystem = false))
      collectionsExplicitSystemResult must be right

      val collectionsExplicitSystem = collectionsExplicitSystemResult.right.get
      collectionsExplicitSystem === collections
    }

    "show collections(including system)" in new WithFreshDb {
      val collectionsResult = await(Collection.all(withSystem = true))
      collectionsResult must be right

      val collections = collectionsResult.right.get
      collections.collections must have size 14
    }


    "do nothing if try to delete not existing collection" in new WithFreshDb {
      val collectionsBefore = await(Collection.all(withSystem = true))
      val dropResult = await(Collection.drop(name = notExistingCollectionName))
      dropResult must be left

      val error = dropResult.left.get
      error.code === 404
      error.errorNum === 1203


      val collectionsAfter = await(Collection.all(withSystem = true))
      collectionsBefore === collectionsAfter
    }

    "delete proper collection" in new WithFreshDb {
      val collectionInfo = await(Collection.info(name = collectionForTest))
      val deleteResult = await(Collection.drop(name = collectionForTest))
      deleteResult must be right

      deleteResult.right.get === collectionInfo.right.get.id

      val infoResult = await(Collection.info(name = collectionForTest))
      infoResult must be left

      val error = infoResult.left.get
      error.code === 404
      error.errorNum === 1203
    }

    "return error if try to truncate not existing collection" in new WithFreshDb {
      val truncateResult = await(Collection.truncate(name = notExistingCollectionName))
      truncateResult must be left

      val error = truncateResult.left.get
      error.code === 404
      error.errorNum === 1203
    }

    "truncate collection" in new WithFreshDb {
      val collectionInfo = await(Collection.info(name = collectionForTest))
      val truncateResult = await(Collection.truncate(name = collectionForTest))
      truncateResult must be right

      val collectionInfoId = collectionInfo.right.get.id
      val truncateResultId = truncateResult.right.get.id

      truncateResultId === collectionInfoId

      await(Collection.documentsCount(name = collectionForTest)).right.get === 0L
    }

    "create new collection" in new WithFreshDb {
      await(Collection.create(CollectionCreateProperties(name = notExistingCollectionName))) must be right

      await(Collection.info(name = notExistingCollectionName)) must be right
    }

    "create new system collection with given properties" in new WithFreshDb {
      val collectionCreateProperties = CollectionCreateProperties(
        name = s"_$notExistingCollectionName",
        isSystem = Some(true),
        indexBuckets = Some(32),
        doCompact = Some(false)
      )
      await(Collection.create(collectionCreateProperties)) must be right

      val createdCollectionPropertiesResult = await(Collection.properties(name = s"_$notExistingCollectionName"))
      createdCollectionPropertiesResult must be right

      val createdCollectionProperties = createdCollectionPropertiesResult.right.get
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
      await(Collection.create(collectionCreateProperties)) must be right

      val createdCollectionPropertiesResult = await(Collection.properties(name = notExistingCollectionName))
      createdCollectionPropertiesResult must be right

      val createdCollectionProperties = createdCollectionPropertiesResult.right.get
      createdCollectionProperties.isSystem === false
      createdCollectionProperties.indexBuckets === 13
      createdCollectionProperties.doCompact === true
    }

    "fail to create new collection with existing name" in new WithFreshDb {
      val createResult = await(Collection.create(CollectionCreateProperties(name = collectionForTest)))
      createResult must be left

      val error = createResult.left.get
      error.code === 409
      error.errorNum === 1207
    }

    "fail to create new collection with invalid properties" in new WithFreshDb {
      val createResult = await(Collection.create(CollectionCreateProperties(name = notExistingCollectionName, journalSize = Some(666))))
      createResult must be left

      val error = createResult.left.get
      error.code === 400
      error.errorNum === 10
    }

    "return error if try to load not existing collection" in new WithFreshDb {
      val loadResult = await(Collection.load(name = notExistingCollectionName))
      loadResult must be left

      val error = loadResult.left.get
      error.code === 404
      error.errorNum === 1203
    }

    "load collection" in new WithFreshDb {
      val loadResult = await(Collection.load(name = collectionForTest))
      loadResult must be right

      loadResult.right.get.status === 3
    }

    "return error if try to unload not existing collection" in new WithFreshDb {
      val unloadResult = await(Collection.unload(name = notExistingCollectionName))
      unloadResult must be left

      val error = unloadResult.left.get
      error.code === 404
      error.errorNum === 1203
    }

    "unload collection" in new WithFreshDb {
      val unloadResult = await(Collection.unload(name = collectionForTest))
      unloadResult must be right

      unloadResult.right.get.status === 2 or unloadResult.right.get.status === 4
    }

    "return error if try to rename not existing collection" in new WithFreshDb {
      val renameResult = await(Collection.rename(name = notExistingCollectionName, newName = s"$notExistingCollectionName-renamed"))
      renameResult must be left

      val error = renameResult.left.get
      error.code === 404
      error.errorNum === 1203
    }

    "not rename collection to if new name used by another existing collection" in new WithFreshDb {
      val renameResult = await(Collection.rename(name = collectionForTest, newName = emptyCollection))
      renameResult must be left

      val error = renameResult.left.get
      error.code === 409
      error.errorNum === 1207
    }

    "rename collection" in new WithFreshDb {
      val beforeRenameInfo = await(Collection.info(name = collectionForTest)).right.get
      val renameResult = await(Collection.rename(name = collectionForTest, newName = notExistingCollectionName))
      renameResult must be right

      renameResult.right.get.id === beforeRenameInfo.id

      await(Collection.info(name = collectionForTest)) must be left
    }

    "return error if try to rotate journal for not existing collection" in new WithFreshDb {
      val rotateResult = await(Collection.rotate(name = notExistingCollectionName))
      rotateResult must be left

      val error = rotateResult.left.get
      error.code === 404
      error.errorNum === 1203
    }

    "not rotate not existing collection journal" in new WithFreshDb {
      val rotateResult = await(Collection.rotate(name = collectionForTest))
      rotateResult must be right

      rotateResult.right.get === false
    }

    "return error if try to set waitForSync for not existing collection" in new WithFreshDb {
      val setWaitForSyncResult = await(Collection.setWaitForSync(name = notExistingCollectionName, waitForSync = false))
      setWaitForSyncResult must be left

      val error = setWaitForSyncResult.left.get
      error.code === 404
      error.errorNum === 1203
    }

    "update waitForSync property" in new WithFreshDb {
      val setWaitForSyncResult = await(Collection.setWaitForSync(name = collectionForTest, waitForSync = true))
      setWaitForSyncResult must be right

      setWaitForSyncResult.right.get.waitForSync === true
    }

    "return error if try to set journalSize for not existing collection" in new WithFreshDb {
      val setJournalSizeResult = await(Collection.setJournalSize(name = notExistingCollectionName, journalSize = 1048576L))
      setJournalSizeResult must be left

      val error = setJournalSizeResult.left.get
      error.code === 404
      error.errorNum === 1203
    }

    "fail to update collection's properties with invalid journal size" in new WithFreshDb {
      val setJournalSizeResult = await(Collection.setJournalSize(name = collectionForTest, journalSize = 666))
      setJournalSizeResult must be left

      val error = setJournalSizeResult.left.get
      error.code === 400
      error.errorNum === 10
    }

    "update journal size" in new WithFreshDb {
      private val newJournalSize = 1048576L
      val setJournalSizeResult = await(Collection.setJournalSize(name = collectionForTest, journalSize = newJournalSize))
      setJournalSizeResult must be right

      setJournalSizeResult.right.get.journalSize === newJournalSize
    }
  }
}
