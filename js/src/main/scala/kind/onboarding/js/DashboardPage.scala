package kind.onboarding.js

import scala.scalajs.js.annotation.JSExportAll
import kind.logic.*
import scala.scalajs.js.JSConverters.*

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
