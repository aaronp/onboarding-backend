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

    def saveDraft(json : Json): DocId = {
      service.saveDoc(json).execOrThrow() match {
        case result: ActionResult => fail(s"Expected a failed result, not " + result)
        case id : DocId => id
      }
    }
  }

  "OnboardingService.approve" should {
    "be able to approve documents" in {

      val underTest = new UnderTest
      import underTest.*


      //          service.saveDoc("invalid".withKey("example"))

    }
    "be able to approve previously rejected documents" in {

      val underTest = new UnderTest
      import underTest.*

    }
  }
  "OnboardingService.save" should {

    "reject documents without name and category fields" in {
      val underTest = new UnderTest
      import underTest.*

      service.saveDoc("invalid".withKey("example")).execOrThrow() match {
        case result: ActionResult => result.success shouldBe false
        case other => fail(s"Expected a failed result, not " + other)
      }
    }
    "be able to save and update version documents" in {
      val underTest = new UnderTest
      import underTest.*

      val doc = DraftDoc("hello", "transport", "submarines")
      val id = underTest.saveDraft(doc.merge("extra".withKey("info")))
      println(underTest.database.formatted)

      val id2 = underTest.saveDraft(doc.merge("more".withKey("information")))

      id shouldBe id2

      println("wtf")

      val Some(found) = database.at("drafts".asPath)
      println("found")
      println(found.formatted)
      println("...")
      val check = database.query("drafts".asPath, None)
      println(check)

      service.listDocs().execOrThrow().size shouldBe 1
    }
    "not allow already-approved drafts" in {

      val underTest = new UnderTest
      import underTest.*

    }
  }
}
