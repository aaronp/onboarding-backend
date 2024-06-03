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
}
