package play.modules.arangodb

object CollectionsApiSpec extends PlayArangoSpec {

  lazy val api = validApplication.injector.instanceOf[ArangoApi]

  s"Collections" title

  private val notExistingCollectionName: String = "not-existing-collection"

  private val collectionForTest: String = "_users"

  "Arango API" should {
    "return none for info with not existing collection" in {
      await(api.collectionInfo(name = notExistingCollectionName)) must be empty
    }

    "return collection info" in {
      val collectionInfoResult = await(api.collectionInfo(name = collectionForTest))
      collectionInfoResult must be some
      val collectionInfo = collectionInfoResult.get
      collectionInfo.name must be_==(collectionForTest)
      collectionInfo.isSystem must be_==(true)
    }

    "return none if call count for not existing collection" in {
      await(api.collectionDocumetsCount(name = notExistingCollectionName)) must be empty
    }

    "count documents number in collection" in {
      await(api.collectionDocumetsCount(name = collectionForTest)) getOrElse 0L must be_>(0L)
    }

    "return none for properties with not existing collection" in {
      await(api.collectionProperties(name = notExistingCollectionName)) must be empty
    }

    "return collection properties" in {
      val collectionPropertiesResult = await(api.collectionProperties(name = collectionForTest))
      collectionPropertiesResult must be some
      val collectionProperties = collectionPropertiesResult.get
      collectionProperties.name must be_==(collectionForTest)
      collectionProperties.figures must be empty
    }

    "return none for statistics with not existing collection" in {
      await(api.collectionStatistics(name = notExistingCollectionName)) must be empty
    }

    "return collection statistics" in {
      val collectionStatisticsResult = await(api.collectionStatistics(name = collectionForTest))
      collectionStatisticsResult must be some
      val collectionStatistics = collectionStatisticsResult.get
      collectionStatistics.name must be_==(collectionForTest)
      collectionStatistics.figures must be some
    }

    "return none for revision with not existing collection" in {
      await(api.collectionRevision(name = notExistingCollectionName)) must be empty
    }

    "return collection revision" in {
      val collectionRevisionResult = await(api.collectionRevision(name = collectionForTest))
      collectionRevisionResult must be some
      val collectionRevision = collectionRevisionResult.get
      collectionRevision.name must be_==(collectionForTest)
      collectionRevision.revision must be some
    }

    "return none for checksum with not existing collection" in {
      await(api.collectionChecksum(name = notExistingCollectionName)) must be empty
    }

    "return collection checksum" in {
      val ch1Result = await(api.collectionChecksum(name = collectionForTest))
      ch1Result must be some
      val ch1 = ch1Result.get.checksum
      ch1 must be some
      val ch2Result = await(api.collectionChecksum(name = collectionForTest, withRevisions = true))
      ch2Result must be some
      val ch2 = ch2Result.get.checksum
      ch2 must be some
      val ch3Result = await(api.collectionChecksum(name = collectionForTest, withData = true))
      ch3Result must be some
      val ch3 = ch3Result.get.checksum
      ch3 must be some
      val ch4Result = await(api.collectionChecksum(name = collectionForTest, withRevisions = true, withData = true))
      ch4Result must be some
      val ch4 = ch4Result.get.checksum
      ch4 must be some
      val ch5Result = await(api.collectionChecksum(name = collectionForTest, withRevisions = false, withData = false))
      ch5Result must be some
      val ch5 = ch5Result.get.checksum
      ch5 must be some

      ch1 must be_!=(ch2)
      ch1 must be_!=(ch3)
      ch1 must be_!=(ch4)
      ch2 must be_!=(ch3)
      ch2 must be_!=(ch4)
      ch3 must be_!=(ch4)
      ch1 must be_==(ch5)
    }

    "return none if try to call not existing DB" in {
      val api = validApplication.configure("play.arangodb.db" -> "play-arangodb-tests-not-existing").injector.instanceOf[ArangoApi]
      await(api.collections()) must be empty
    }

    "show non-system collections" in {
      val collectionsResult = await(api.collections())
      collectionsResult must be some
      val collections = collectionsResult.get
      collections.collections must have size 0
      val collectionsExplicitSystemResult = await(api.collections(withSystem = false))
      collectionsExplicitSystemResult must be some
      val collectionsExplisitSystem = collectionsExplicitSystemResult.get
      collectionsExplisitSystem must be_==(collections)
    }

    "show collections(including system)" in {
      val collectionsResult = await(api.collections(withSystem = true))
      collectionsResult must be some
      val collections = collectionsResult.get
      collections.collections must not have size(0)
    }

  }
}
