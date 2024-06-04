package kind.onboarding.js

import kind.logic._

import scala.scalajs.js.JSConverters._
import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
case class DashboardPage(services: Services) {

  def draftsForUser(user: String) = {
    services.bff.listDraftsForUser(user).getAsJS { drafts =>
      drafts.map(_.asJSON).toJSArray
    }
  }

  def withdrawDraft(draftId: JS) = {
    println(s"withdrawDraft(${draftId})")
    services.bff.withdraw(draftId.toString(), true).execOrThrow() match {
      case result: ActionResult => result.asJSON
      case Some(json)           => json.asJSON
      case None                 => ActionResult.fail(s"draft '${draftId}' not found").asJSON
    }
  }
}
