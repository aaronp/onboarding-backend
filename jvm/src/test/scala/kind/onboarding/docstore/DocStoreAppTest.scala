package kind.onboarding.docstore

import org.scalatest.wordspec.AnyWordSpec
import kind.logic.telemetry.*
import kind.onboarding.docstore.model.*
import org.scalatest.matchers.should.Matchers

class DocStoreAppTest extends AnyWordSpec with Matchers {

  "DocStoreApp.saveDocumentVersioned, getDocumentLatest" should {
    "save a new entry for a document" in {
      val app = DocStoreApp.inMemory(using Telemetry())

      // start with some data at a given path ... version 1
      val originalPath =
        app.saveDocumentVersioned("original/document", ujson.Obj("hello" -> "World"))
      originalPath shouldBe "original/document/v0"
      app.getDocumentLatest("original/document") shouldBe ujson.Obj("hello" -> "World")

      // overrwrite it at the same path ... version 2
      val newVersionPath =
        app.saveDocumentVersioned("original/document", ujson.Obj("over" -> "written"))
      newVersionPath shouldBe "original/document/v1"
      app.getDocumentLatest("original/document") shouldBe ujson.Obj("over" -> "written")

      // update it at the same path ... version 3
      val updatedPath = app.upsertDocumentVersioned("original/document", ujson.Obj("up" -> "dated"))
      updatedPath shouldBe "original/document/v2"
      app.getDocumentLatest("original/document") shouldBe ujson.Obj(
        "over" -> "written",
        "up"   -> "dated"
      )

      // check our versions
      app.listChildren("original/document") should contain only ("v0", "v1", "v2")

      // check the first version (two ways)
      app.getDocument("original/document", Option("v0")) shouldBe ujson.Obj("hello" -> "World")
      app.getDocument(originalPath, None) shouldBe ujson.Obj("hello" -> "World")

      // check the second version (two ways)
      app.getDocument("original/document", Option("v1")) shouldBe ujson.Obj("over" -> "written")
      app.getDocument(newVersionPath, None) shouldBe ujson.Obj("over" -> "written")

      // check the last version (two ways)
      app.getDocument("original/document", Option("v2")) shouldBe ujson.Obj(
        "over" -> "written",
        "up"   -> "dated"
      )
      app.getDocument(updatedPath, None) shouldBe ujson.Obj("over" -> "written", "up" -> "dated")

      val CompareDocuments200Response(Some(diff)) =
        app.compareDocuments(CompareDocumentsRequestData(originalPath, updatedPath).validated().get)
      val expectedDiff = ujson.read("""{
                                      |  "hello": {
                                      |    "removed": "World"
                                      |  },
                                      |  "over": {
                                      |    "added": "written"
                                      |  },
                                      |  "up": {
                                      |    "added": "dated"
                                      |  }
                                      |}""".stripMargin)
      diff shouldBe expectedDiff
    }
  }
  "DocStoreApp.listChildren" should {

    "return an empty list for nonexistent nodes" in {
      val app = DocStoreApp.inMemory(using Telemetry())
      app.listChildren("") should be(empty)
      app.listChildren("/") should be(empty)
      app.listChildren("/what/ever") should be(empty)
    }
    "return the root nodes" in {
      val app = DocStoreApp.inMemory(using Telemetry())
      app.listChildren("") should be(empty)
      app.listChildren("/") should be(empty)

      app.saveDocument("first/nested", ujson.Obj("hello" -> "World"))
      app.saveDocument("second", ujson.Obj("hello" -> "World"))

      app.listChildren("") should contain only ("first", "second")
      app.listChildren("/") should contain only ("first", "second")
    }
    "be able to list the children" in {
      val app = DocStoreApp.inMemory(using Telemetry())
      app.saveDocument("a/b/c1", ujson.Obj("hello" -> "World"))
      app.saveDocument("a/b/c2", ujson.Obj("second" -> "child"))
      app.saveDocument("a/b/c3", ujson.Obj("third" -> "kid"))

      app.listChildren("a/b") should contain only ("c1", "c2", "c3")
      app.listChildren("a") should contain only ("b")
    }
  }
}
