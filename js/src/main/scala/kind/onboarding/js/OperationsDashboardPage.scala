package kind.onboarding.js

import scala.scalajs.js.annotation._
import kind.logic.*
import scala.scalajs.js.JSConverters._
import kind.onboarding.svc.*

@JSExportAll
case class OperationsDashboardPage(services: Services) {

  /** @return
    *   the documents either do not have an 'approved' field or they have one which is set to false
    */
  def listUnapprovedDrafts() = {
    services.bff
      .listDrafts()
      .execOrThrow()
      .flatMap { draftData =>
        draftData("data").as[HasApproved].toOption match {
          case None                       => Option(draftData.asJSON)
          case Some(doc) if !doc.approved => Option(draftData.asJSON)
          case _                          => None
        }
      }
      .toJSArray
  }

  /** @return
    *   the approved documents
    */
  def listApprovedDrafts() = {
    services.bff
      .listDrafts()
      .execOrThrow()
      .flatMap { draftData =>
        draftData("data")
          .as[HasApproved]    //
          .toOption           //
          .filter(_.approved) //
          .map(_.asJSON)
      }
      .toJSArray
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
