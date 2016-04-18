package play.modules
package arangodb

object DocumentApiSpec extends PlayArangoSpec {

  lazy val DocumentApi = validApplication.injector.instanceOf[ArangoApi].Document

  private val notExistingCollectionName = "not-existing-collection"

  private val collectionForTest = "test-collection"

  private val emptyCollection = "empty-collection"

  "Documents" title

  "Arango API" should {
    "return error if try to read not existing document" in new WithFreshDb {
      val docResult = await(DocumentApi.read(handle = s"$emptyCollection/1"))
      docResult must be left

      val error = docResult.left.get
      error.code === 404
      error.errorNum === 1203
    }

    "return error if try to remove not existing document" in new WithFreshDb {
      val removeResult = await(DocumentApi.remove(handle = s"$emptyCollection/1"))
      removeResult must be left

      val error = removeResult.left.get
      error.code === 404
      error.errorNum === 1202
    }

    "remove document" in new WithFreshDb {
      private val docHandle = s"$collectionForTest/107235191229"
      val removeDocumentResult = await(DocumentApi.remove(handle = docHandle))

      removeDocumentResult must be right

      removeDocumentResult.right.get._id === docHandle
    }

    "not remove document if revision in ArangoDB not the same as provided" in new WithFreshDb {
      private val docHandle = s"$collectionForTest/107235191229"
      val removeDocumentResult = await(DocumentApi.remove(handle = docHandle, revision = Some("666")))

      removeDocumentResult must be left
      val error = removeDocumentResult.left.get
      error.code === 412
      error.errorNum === 1200
      error._id.get === docHandle
    }

    "return error if try to get revision for not existing document" in new WithFreshDb {
      val revisionResult = await(DocumentApi.revision(handle = s"$emptyCollection/1"))
      revisionResult must be left

      val error = revisionResult.left.get
      error.code === 404
      error.errorNum === 0
    }

    "return document revision" in new WithFreshDb {
      val docRevision = await(DocumentApi.revision(handle = s"$collectionForTest/107235191229"))

      docRevision must be right
      // can't check actual value due to restore process assign new revision for doc
    }

    "return error when try to get all documents from not existing collection" in new WithFreshDb {
      val allResult = await(DocumentApi.all(collection = notExistingCollectionName))
      allResult must be left

      val error = allResult.left.get
      error.code === 404
      error.errorNum === 1203
    }

    "return empty result when get all documents from empty collection" in new WithFreshDb {
      val allResult = await(DocumentApi.all(collection = emptyCollection))

      allResult must be right

      allResult.right.get.documents must have size 0
    }

    val keysSorted = Seq("107230079421", "107235191229").sorted

    "return keys by default when get all documents" in new WithFreshDb {
      val allResult = await(DocumentApi.all(collection = collectionForTest))

      allResult must be right

      val documents = allResult.right.get.documents
      documents must have size keysSorted.size

      documents.sorted === keysSorted
    }

    "return keys when get all documents" in new WithFreshDb {
      val allResult = await(DocumentApi.all(collection = collectionForTest, returnIds = false))

      allResult must be right

      val documents = allResult.right.get.documents
      documents must have size keysSorted.size

      documents.sorted === keysSorted
    }

    "return ids when get all documents with 'returnIds' flag" in new WithFreshDb {
      val allResult = await(DocumentApi.all(collection = collectionForTest, returnIds = true))

      allResult must be right

      val documents = allResult.right.get.documents
      val idsSorted = Seq(s"$collectionForTest/107230079421", s"$collectionForTest/107235191229").sorted
      documents must have size idsSorted.size

      documents.sorted === idsSorted

    }

  }
}
