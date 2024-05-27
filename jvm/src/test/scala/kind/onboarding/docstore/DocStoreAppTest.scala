package kind.onboarding.docstore

import org.scalatest.wordspec.AnyWordSpec
import kind.logic.telemetry.*
import org.scalatest.matchers.should.Matchers

class DocStoreAppTest extends AnyWordSpec with Matchers {

  "DocStoreApp" should {

    "return an empty list for nonexistent nodes" in {
      val app = DocStoreApp()(using Telemetry())
      app.listChildren("") should be(empty)
      app.listChildren("/") should be(empty)
      app.listChildren("/what/ever") should be(empty)
    }
    "return the root nodes" in {
      val app = DocStoreApp()(using Telemetry())
      app.listChildren("") should be(empty)
      app.listChildren("/") should be(empty)

      app.saveDocument("first/nested", ujson.Obj("hello" -> "World"))
      app.saveDocument("second", ujson.Obj("hello" -> "World"))

      app.listChildren("") should contain only("first", "second")
      app.listChildren("/") should contain only("first", "second")
    }
    "be able to list the children" in {
      val app = DocStoreApp()(using Telemetry())
      app.saveDocument("a/b/c1", ujson.Obj("hello" -> "World"))
      app.saveDocument("a/b/c2", ujson.Obj("second" -> "child"))
      app.saveDocument("a/b/c3", ujson.Obj("third" -> "kid"))

      app.listChildren("a/b") should contain only("c1", "c2", "c3")
      app.listChildren("a") should contain only("b")
    }
  }
}
