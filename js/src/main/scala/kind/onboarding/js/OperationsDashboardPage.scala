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
        val withdrawn = draftData.asHasWithdrawn.fold(false)(_.withdrawn)
        draftData.asHasApproved match {
          case None if !withdrawn                       => Option(draftData.asJSON)
          case Some(doc) if !doc.approved && !withdrawn => Option(draftData.asJSON)
          case _                                        => None
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
        val withdrawn = draftData.asHasWithdrawn.fold(false)(_.withdrawn)
        draftData.asHasApproved.filter(d => d.approved && !withdrawn).map(_ => draftData.asJSON)
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

  def unapproveDraft(draftId: JS) = doApproveDraft(draftId, false)

  def approveDraft(draftId: JS) = doApproveDraft(draftId, true)

  private def doApproveDraft(draftId: JS, approved: Boolean) = {
    services.bff.approve(draftId.toString(), approved).execOrThrow() match {
      case result: ActionResult => result.asJSON
      case Some(json)           => json.asJSON
      case None                 => ActionResult.fail(s"draft '${draftId}' not found").asJSON
    }
  }
}
