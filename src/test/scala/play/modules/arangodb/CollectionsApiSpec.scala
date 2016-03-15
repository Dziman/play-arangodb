package play.modules.arangodb

object CollectionsApiSpec extends PlayArangoSpec {

  lazy val Collection = validApplication.injector.instanceOf[ArangoApi].Collection

  s"Collections" title

  private val notExistingCollectionName: String = "not-existing-collection"

  private val collectionForTest: String = "test-collection"

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
      await(Collection.documentsCount(name = "empty-collection")) getOrElse -1L === 0L
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

  }
}
