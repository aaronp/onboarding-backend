package kind.onboarding.js

import scala.scalajs.js.annotation._
import kind.logic.*
import kind.logic.json.*
import scala.scalajs.js.JSConverters._
import kind.onboarding.svc.*

@JSExportAll
case class OperationsDashboardPage(services: Services) {

//   @JSExport("listUnapprovedDrafts")
  def listUnapprovedDrafts() = {
    import scala.scalajs.js.JSConverters._

    // the documents either do not have an 'approved' field or they have one which is set to false
    val unapproved = services.bff.listDrafts().execOrThrow().filter { draftData =>
      draftData.as[HasApproved].toOption.fold(true)(!_.approved)
    }
    unapproved.map(_.asJSON).toJSArray
  }

  def withdrawDraft(draftId: JS) = {
    services.bff.withdraw(draftId.toString(), true).execOrThrow() match {
      case result: ActionResult => result.asJSON
      case Some(json)           => json.asJSON
      case None                 => ActionResult.fail(s"draft '${draftId}' not found").asJSON
    }
  }

  def approveDraft(draftId: JS) = {
    services.bff.approve(draftId.toString(), true).execOrThrow() match {
      case result: ActionResult => result.asJSON
      case Some(json)           => json.asJSON
      case None                 => ActionResult.fail(s"draft '${draftId}' not found").asJSON
    }
  }
}
