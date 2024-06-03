package kind.onboarding.svc

import kind.logic.*
import kind.logic.json.*
import kind.logic.telemetry.*
import kind.onboarding.docstore.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class OnboardingServiceTest extends AnyWordSpec with Matchers {

  class UnderTest {
    val telemetry = Telemetry()
    val handler   = DocStoreHandler()
    val docStore  = DocStoreApp(handler)(using telemetry)
    val service   = OnboardingService(docStore)(using telemetry)

    def database: PathTree = handler.asTree.execOrThrow()

    def saveDraft(json: Json): DocId = {
      service.saveDoc(json).execOrThrow() match {
        case result: ActionResult => fail(s"Expected a failed result, not " + result)
        case id: DocId            => id
      }
    }
  }

  "OnboardingService.approve" should {
    "be able to approve documents" in {

      val underTest = new UnderTest
      import underTest.*

      val id = underTest.saveDraft(DraftDoc("foo", "transport", "submarines").asUJson)
      underTest.saveDraft(
        DraftDoc("foo", "transport", "submarines").merge("more".withKey("data"))
      ) shouldBe id

      val Some(doc) = service.approve(id, true).execOrThrow()
      doc shouldBe """{
        "approved": true,
        "subCategory": "submarines",
        "data": "more",
        "name": "foo",
        "category": "transport"
      }""".parseAsJson

    }
    "be able to approve previously rejected documents" in {

      val underTest = new UnderTest
      import underTest.*

      val id = underTest.saveDraft(DraftDoc("foo", "transport", "submarines").asUJson)
      underTest.saveDraft(
        DraftDoc("foo", "transport", "submarines").merge("more".withKey("data"))
      ) shouldBe id

      val Some(doc) = service.approve(id, false).execOrThrow()

      service.listApprovedDocs().execOrThrow().size shouldBe 0
      val Some(doc2) = service.approve(id, true).execOrThrow()

      println("-" * 80)
      println(database.formatted)
      println("-" * 80)

      val Seq(approvedDoc) = service.listApprovedDocs().execOrThrow()
      println(approvedDoc)
    }
  }
  "OnboardingService.listDrafts" should {
    "return the latest documents" in {

      val underTest = new UnderTest
      import underTest.*

      service.listDrafts().execOrThrow().size shouldBe 0
      val id1 = underTest.saveDraft(DraftDoc("hello", "transport", "submarines").asUJson)
      service.listDrafts().execOrThrow().size shouldBe 1
      val id2        = underTest.saveDraft(DraftDoc("there", "transport", "submarines").asUJson)
      val updatedDoc = underTest.saveDraft(DraftDoc("there", "changed", "toThis").asUJson)

      val Seq(a, b) = service.listDrafts().execOrThrow()
      a shouldBe """{
          "data": {
            "name": "hello",
            "category": "transport",
            "subCategory": "submarines"
          },
          "_id": "hello"
        }""".parseAsJson

      b shouldBe """{
        "data": {
          "subCategory": "toThis",
          "name": "there",
          "category": "changed"
        },
        "_id": "there"
      }""".parseAsJson

    }
  }
  "OnboardingService.save" should {

    "reject documents without name and category fields" in {
      val underTest = new UnderTest
      import underTest.*

      service.saveDoc("invalid".withKey("example")).execOrThrow() match {
        case result: ActionResult => result.success shouldBe false
        case other                => fail(s"Expected a failed result, not " + other)
      }
    }
    "be able to save and update version documents" in {
      val underTest = new UnderTest
      import underTest.*

      // save an initial draft
      val doc = DraftDoc("hello", "transport", "submarines")
      val id  = underTest.saveDraft(doc.merge("extra".withKey("info")))

      // now update it
      val id2 = underTest.saveDraft(doc.merge("more".withKey("information")))

      // the ID should be the same
      id shouldBe id2

      // we should get the latest version
      val Seq(latest) = service.listDrafts().execOrThrow()
      latest shouldBe """{
          "data": {
            "subCategory": "submarines",
            "name": "hello",
            "information": "more",
            "category": "transport",
            "info": "extra"
          },
          "_id": "hello"
        }""".parseAsJson

    }
    "not allow already-approved drafts" in {
      val underTest = new UnderTest
      import underTest.*

      underTest.service
        .saveDoc(DraftDoc("hello", "transport", "submarines").approve(true).asUJson)
        .execOrThrow() match {
        case result: ActionResult =>
          result.success shouldBe false

          result.message should startWith("Rejected as doc 'hello' is already approved")
        case result: Json => fail("save should have failed. You have to use the approve services")
      }

      service.listDrafts().execOrThrow().size shouldBe 0
    }
  }
}
