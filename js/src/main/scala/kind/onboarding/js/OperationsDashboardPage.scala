package kind.onboarding.js

import scala.scalajs.js.annotation._
import kind.logic.*
import kind.logic.json.*
import scala.scalajs.js.JSConverters._

@JSExportAll
case class OperationsDashboardPage(services: Services) {

  def listDrafts() = {
    import scala.scalajs.js.JSConverters._

    services.bff.listDrafts().execOrThrow().map(_.asJSON).toJSArray
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
