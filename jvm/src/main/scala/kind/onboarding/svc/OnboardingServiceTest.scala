package kind.onboarding.svc


import org.scalatest.wordspec.AnyWordSpec
import kind.logic.telemetry.*
import kind.onboarding.docstore.model.*
import org.scalatest.matchers.should.Matchers

class OnboardingServiceTest extends AnyWordSpec with Matchers {

    "OnboardingService.approve" should {
        "be able to approve documents" in {

        }
        "be able to approve previously rejected documents" in {

        }
    }
  "OnboardingService.save" should {
    "be able to save version documents" in {
      val docStore = DocStoreApp()(using Telemetry())

    }
    "not allow already-approved drafts" in {
      val docStore = DocStoreApp()(using Telemetry())

    }
}