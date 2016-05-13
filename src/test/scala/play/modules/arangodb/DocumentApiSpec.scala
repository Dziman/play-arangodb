package play.modules
package arangodb

import play.api.libs.json.Json
import play.modules.arangodb.model._

object DocumentApiSpec extends PlayArangoSpec {

  implicit val docReads = Json.reads[TestClass]
  implicit val docWrites = Json.writes[TestClass]

  implicit val doc2Reads = Json.reads[TestClass2]
  implicit val doc2Writes = Json.writes[TestClass2]

  implicit val doc3Reads = Json.reads[TestClass3]
  implicit val doc3Writes = Json.writes[TestClass3]

  implicit val docMixReads = Json.reads[TestClassMix]
  implicit val docMixWrites = Json.writes[TestClassMix]

  lazy val DocumentApi = validApplication.injector.instanceOf[ArangoApi].Document

  private val notExistingCollectionName = "not-existing-collection"

  private val collectionForTest = "test-collection"

  private val emptyCollection = "empty-collection"

  "Documents" title

  "Arango API" should {
    "return error if try to read document from not existing collection" in new WithFreshDb {
      val docResult = await(DocumentApi.read[TestClass](handle = s"$notExistingCollectionName/1"))
      docResult must be left

      val error = docResult.left.get
      error.code === 404
      error.errorNum === 1203
    }

    "return error if try to read not existing document" in new WithFreshDb {
      val docResult = await(DocumentApi.read[TestClass](handle = s"$emptyCollection/1"))
      docResult must be left

      val error = docResult.left.get
      error.code === 404
      error.errorNum === 1202
    }

    "return document" in new WithFreshDb {
      val docResult = await(DocumentApi.read[TestClass](s"$collectionForTest/107230079421"))
      docResult must be right
      val doc = docResult.right.get
      doc.price === 100
      doc.name ==="doc1"
    }

    "return document if its revision not match given noneMatch revision" in new WithFreshDb {
      val docResult = await(DocumentApi.read[TestClass](s"$collectionForTest/107230079421", Some(Left("666"))))
      docResult must be right
      val doc = docResult.right.get
      doc.price === 100
      doc.name === "doc1"
    }

    "return error on read if its revision match given noneMatch revision" in new WithFreshDb {
      val revision = await(DocumentApi.revision(s"$collectionForTest/107230079421")).right.get
      val docResult = await(DocumentApi.read[TestClass](s"$collectionForTest/107230079421", Some(Left(revision))))
      docResult must be left

      val error = docResult.left.get
      error.code === 304
      error.errorNum === 0
    }

    "return error on read if its revision not match given match revision" in new WithFreshDb {
      val docResult = await(DocumentApi.read[TestClass](s"$collectionForTest/107230079421", Some(Right("666"))))
      docResult must be left

      val error = docResult.left.get
      error.code === 412
      error.errorNum === 1200
    }

    "return document if its revision match given match revision" in new WithFreshDb {
      val revision = await(DocumentApi.revision(s"$collectionForTest/107230079421")).right.get
      val docResult = await(DocumentApi.read[TestClass](s"$collectionForTest/107230079421", Some(Right(revision))))
      docResult must be right

      val doc = docResult.right.get
      doc.price === 100
      doc.name === "doc1"
    }

    "return error if try to create document in not existing collection" in new WithFreshDb {
      val docResult = await(DocumentApi.create[TestClass](collection = notExistingCollectionName, TestClass(150, "name1")))
      docResult must be left

      val error = docResult.left.get
      error.code === 404
      error.errorNum === 1203
    }

    "create new document" in new WithFreshDb {
      private val testClass = TestClass(150, "name1")
      val docResult = await(DocumentApi.create[TestClass](collection = emptyCollection, testClass))
      docResult must be right

      val readResult =  await(DocumentApi.read[TestClass](s"$emptyCollection/${docResult.right.get._key}"))
      readResult must be right

      readResult.right.get === testClass
    }

    "create new document waitForSync false" in new WithFreshDb {
      private val testClass = TestClass(150, "name1")
      val docResult = await(DocumentApi.create[TestClass](collection = emptyCollection, testClass, waitForSync = Some(false)))
      docResult must be right

      val readResult = await(DocumentApi.read[TestClass](s"$emptyCollection/${docResult.right.get._key}"))
      readResult must be right

      readResult.right.get === testClass
    }

    "create new document waitForSync true" in new WithFreshDb {
      private val testClass = TestClass(150, "name1")
      val docResult = await(DocumentApi.create[TestClass](collection = emptyCollection, testClass, waitForSync = Some(true)))
      docResult must be right

      val readResult = await(DocumentApi.read[TestClass](s"$emptyCollection/${docResult.right.get._key}"))
      readResult must be right

      readResult.right.get === testClass
    }

    "return error if try to replace document from not existing collection" in new WithFreshDb {
      val docResult = await(DocumentApi.replace[TestClass2](handle = s"$notExistingCollectionName/1", doc = TestClass2("kind1", "XL")))
      docResult must be left

      val error = docResult.left.get
      error.code === 404
      error.errorNum === 1203
    }

    "return error if try to replace not existing document" in new WithFreshDb {
      val docResult = await(DocumentApi.replace[TestClass](handle = s"$emptyCollection/1", doc = TestClass(100, "name")))
      docResult must be left

      val error = docResult.left.get
      error.code === 404
      error.errorNum === 1202
    }

    "replace document waitForSync true" in new WithFreshDb {
      val docResult = await(
        DocumentApi.replace[TestClass2](
          handle = s"$collectionForTest/107230079421",
          doc = TestClass2("kind1", "XXS"),
          waitForSync = Some(true)
        )
      )
      docResult must be right
      val docR = docResult.right.get
      docR._key === "107230079421"
    }

    "replace document waitForSync false" in new WithFreshDb {
      val docResult = await(
        DocumentApi.replace[TestClass2](
          handle = s"$collectionForTest/107230079421",
          doc = TestClass2("kind1", "XXS"),
          waitForSync = Some(false)
        )
      )
      docResult must be right
      val docR = docResult.right.get
      docR._key === "107230079421"
    }

    "replace document if its revision does not match given revision and force update" in new WithFreshDb {
      val docResult = await(
        DocumentApi.replace[TestClass2](
          handle = s"$collectionForTest/107230079421",
          doc = TestClass2("kind1", "XXS"),
          revision = Some("666"),
          force = true
        )
      )
      docResult must be right
      val docRes = docResult.right.get
      docRes._key === "107230079421"
    }

    "return error on replace if its revision not match given revision and do not force update" in new WithFreshDb {
      val docResult = await(
        DocumentApi.replace[TestClass](
          handle = s"$collectionForTest/107230079421",
          doc = TestClass(1, "n"),
          revision = Some("666"),
          force = false
        )
      )
      docResult must be left

      val error = docResult.left.get
      error.code === 412
      error.errorNum === 1200
    }

    "replace document if its revision match given revision and force update" in new WithFreshDb {
      val revision = await(DocumentApi.revision(s"$collectionForTest/107230079421")).right.get
      val docResult = await(
        DocumentApi.replace[TestClass2](
          handle = s"$collectionForTest/107230079421",
          doc = TestClass2("kind", "L"),
          revision = Some(revision),
          force = true
        )
      )
      docResult must be right
      val docRes = docResult.right.get
      docRes._key === "107230079421"
    }

    "replace document if its revision match given revision and not force update" in new WithFreshDb {
      val revision = await(DocumentApi.revision(s"$collectionForTest/107230079421")).right.get
      val docResult = await(
        DocumentApi.replace[TestClass2](
          handle = s"$collectionForTest/107230079421",
          doc = TestClass2("kind", "L"),
          revision = Some(revision),
          force = false
        )
      )
      docResult must be right
      val docRes = docResult.right.get
      docRes._key === "107230079421"
    }

    "replace document if no revision provided and force update" in new WithFreshDb {
      val revision = await(DocumentApi.revision(s"$collectionForTest/107230079421")).right.get
      val docResult = await(
        DocumentApi.replace[TestClass2](
          handle = s"$collectionForTest/107230079421",
          doc = TestClass2("kind", "L"),
          force = true
        )
      )
      docResult must be right
      val docRes = docResult.right.get
      docRes._key === "107230079421"
    }

    "return error if try to patch document from not existing collection" in new WithFreshDb {
      val docResult = await(DocumentApi.patch[TestClass2](handle = s"$notExistingCollectionName/1", doc = TestClass2("kind1", "XL")))
      docResult must be left

      val error = docResult.left.get
      error.code === 404
      error.errorNum === 1203
    }

    "return error if try to patch not existing document" in new WithFreshDb {
      val docResult = await(DocumentApi.patch[TestClass2](handle = s"$emptyCollection/1", doc = TestClass2("kind", "M")))
      docResult must be left

      val error = docResult.left.get
      error.code === 404
      error.errorNum === 1202
    }

    "patch document waitForSync true" in new WithFreshDb {
      val docResult = await(
        DocumentApi.patch[TestClass2](
          handle = s"$collectionForTest/107230079421",
          doc = TestClass2("kind1", "XXS"),
          waitForSync = Some(true)
        )
      )
      docResult must be right
      val docR = docResult.right.get
      docR._key === "107230079421"
    }

    "patch document waitForSync false" in new WithFreshDb {
      val docResult = await(
        DocumentApi.patch[TestClass2](
          handle = s"$collectionForTest/107230079421",
          doc = TestClass2("kind1", "XXS"),
          waitForSync = Some(false)
        )
      )
      docResult must be right
      val docR = docResult.right.get
      docR._key === "107230079421"
    }

    "patch document and merge objects" in new WithFreshDb {
      val docResult = await(
        DocumentApi.patch[TestClassMix](
          handle = s"$collectionForTest/377663747069",
          doc = TestClassMix(33, "new name", TestClass3("newKind", None)),
          mergeObjects = true
        )
      )
      docResult must be right
      val docR = docResult.right.get
      docR._key === "377663747069"

      val updatedDocResult = await(DocumentApi.read[TestClassMix](s"$collectionForTest/377663747069"))
      updatedDocResult must be right
      val updatedDoc = updatedDocResult.right.get

      updatedDoc.price === 33
      updatedDoc.name === "new name"
      updatedDoc.subdoc.kind === "newKind"
      updatedDoc.subdoc.size === Some("L")
    }

    "patch document and do not merge objects" in new WithFreshDb {
      val docResult = await(
        DocumentApi.patch[TestClassMix](
          handle = s"$collectionForTest/377663747069",
          doc = TestClassMix(33, "new name", TestClass3("newKind", None)),
          mergeObjects = false
        )
      )
      docResult must be right
      val docR = docResult.right.get
      docR._key === "377663747069"

      val updatedDocResult = await(DocumentApi.read[TestClassMix](s"$collectionForTest/377663747069"))
      updatedDocResult must be right
      val updatedDoc = updatedDocResult.right.get

      updatedDoc.price === 33
      updatedDoc.name === "new name"
      updatedDoc.subdoc.kind === "newKind"
      updatedDoc.subdoc.size must be none
    }

    // TODO Implement checks
    "patch document and keep nulls" in new WithFreshDb {
      val docResult = await(
        DocumentApi.patch[TestClassMix](
          handle = s"$collectionForTest/377663747069",
          doc = TestClassMix(33, "new name", TestClass3("newKind", None)),
          mergeObjects = false,
          keepNull = true
        )
      )
      docResult must be right
      val docR = docResult.right.get
      docR._key === "377663747069"

      val updatedDocResult = await(DocumentApi.read[TestClassMix](s"$collectionForTest/377663747069"))
      updatedDocResult must be right
      val updatedDoc = updatedDocResult.right.get

      updatedDoc.price === 33
      updatedDoc.name === "new name"
      updatedDoc.subdoc.kind === "newKind"
      updatedDoc.subdoc.size must be none
    }

    // TODO Implement checks
    "patch document and do not keep nulls" in new WithFreshDb {
      val docResult = await(
        DocumentApi.patch[TestClassMix](
          handle = s"$collectionForTest/377663747069",
          doc = TestClassMix(33, "new name", TestClass3("newKind", None)),
          mergeObjects = false,
          keepNull = false
        )
      )
      docResult must be right
      val docR = docResult.right.get
      docR._key === "377663747069"

      val updatedDocResult = await(DocumentApi.read[TestClassMix](s"$collectionForTest/377663747069"))
      updatedDocResult must be right
      val updatedDoc = updatedDocResult.right.get

      updatedDoc.price === 33
      updatedDoc.name === "new name"
      updatedDoc.subdoc.kind === "newKind"
      updatedDoc.subdoc.size must be none
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

    val keysSorted = Seq("107230079421", "107235191229", "377663747069").sorted

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
      val idsSorted = Seq(s"$collectionForTest/107230079421", s"$collectionForTest/107235191229", s"$collectionForTest/377663747069").sorted
      documents must have size idsSorted.size

      documents.sorted === idsSorted

    }

  }
}

case class TestClassDocument(price: Int, name: String, _id: String, _key: String, _rev: String) extends Document
case class TestClass(price: Int, name: String)
case class TestClass2(kind: String, size: String)
case class TestClass3(kind: String, size: Option[String])
case class TestClassMix(price: Int, name: String, subdoc: TestClass3)
