package play.modules
package arangodb

object DocumentApiSpec extends PlayArangoSpec {

  lazy val DocumentApi = validApplication.injector.instanceOf[ArangoApi].Document

  private val notExistingCollectionName = "not-existing-collection"

  private val collectionForTest = "test-collection"

  private val emptyCollection = "empty-collection"

  "Documents" title

  "Arango API" should {
    "return none if try to read not existing document" in new WithFreshDb {
      await(DocumentApi.read(handle = s"$emptyCollection/1")) must be empty
    }

    "return none if try to remove not existing document" in new WithFreshDb {
      await(DocumentApi.remove(handle = s"$emptyCollection/1")) must be empty
    }

    "remove document" in new WithFreshDb {
      private val docHandle = s"$collectionForTest/107235191229"
      val removeDocumentResult = await(DocumentApi.remove(handle = docHandle))

      removeDocumentResult must be some

      removeDocumentResult.get.error === false
      removeDocumentResult.get._id === docHandle
    }

    "not remove document if revision in ArangoDB not the same as provided" in new WithFreshDb {
      private val docHandle = s"$collectionForTest/107235191229"
      val removeDocumentResult = await(DocumentApi.remove(handle = docHandle, revision = Some("666")))

      removeDocumentResult must be some

      removeDocumentResult.get.error === true
      removeDocumentResult.get._id === docHandle
    }

    "return none if try to get revision for not existing document" in new WithFreshDb {
      await(DocumentApi.revision(handle = s"$emptyCollection/1")) must be empty
    }

    "return document revision" in new WithFreshDb {
      val docRevision = await(DocumentApi.revision(handle = s"$collectionForTest/107235191229"))

      docRevision must be some
      // can't check actual value due to restore process assign new revision for doc
    }

    "return none when try to get all documents from not existing collection" in new WithFreshDb {
      await(DocumentApi.all(collection = notExistingCollectionName)) must be empty
    }

    "return empty result when get all documents from empty collection" in new WithFreshDb {
      val allResult = await(DocumentApi.all(collection = emptyCollection))

      allResult must be some

      allResult.get.documents must have size 0
    }

    val keysSorted = Seq("107230079421", "107235191229").sorted

    "return keys by default when get all documents" in new WithFreshDb {
      val allResult = await(DocumentApi.all(collection = collectionForTest))

      allResult must be some

      val documents = allResult.get.documents
      documents must have size keysSorted.size

      documents.sorted === keysSorted
    }

    "return keys when get all documents" in new WithFreshDb {
      val allResult = await(DocumentApi.all(collection = collectionForTest, returnIds = false))

      allResult must be some

      val documents = allResult.get.documents
      documents must have size keysSorted.size

      documents.sorted === keysSorted
    }

    "return ids when get all documents with 'returnIds' flag" in new WithFreshDb {
      val allResult = await(DocumentApi.all(collection = collectionForTest, returnIds = true))

      allResult must be some

      val documents = allResult.get.documents
      val idsSorted = Seq(s"$collectionForTest/107230079421", s"$collectionForTest/107235191229").sorted
      documents must have size idsSorted.size

      documents.sorted === idsSorted

    }

  }
}
