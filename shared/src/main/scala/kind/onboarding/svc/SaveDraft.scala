package kind.onboarding.svc

import kind.logic._
import kind.logic.telemetry.Telemetry
import kind.onboarding._
import kind.onboarding.docstore.DocStoreApp
import upickle.default._
import zio._

import util._
import Systems._

private[svc] object SaveDraft {
  def apply(docStore: DocStoreApp, data: Json)(using
      telemetry: Telemetry
  ): Task[DocId | ActionResult] = {
    val action = data.withKey("input").merge("saveDraft".withKey("action"))
    data.as[DraftDoc] match {
      case Success(doc) =>
        val id = asId(doc.name)

        // check so we don't naughtily approve draft docs here
        data.as[ApprovedDoc] match {
          case Success(alreadyApproved) if alreadyApproved.approved =>
            ActionResult
              .fail(s"Rejected as doc '${id}' is already approved: ${data.render(2)}")
              .asTaskTraced(OnboardingSvc.id, DB.id, action)
          case _ =>
            // good - there sholdn't be an 'approved' field at this stage

            docStore
              .upsertDocumentVersioned(s"docs/drafts/${id}", data.withTimestamp())
              .asTaskTraced(OnboardingSvc.id, DB.id, action)
              .as(id)
        }

      case Failure(err) =>
        ActionResult
          .fail(s"Failed to parse document: ${err.getMessage}")
          .asTaskTraced(OnboardingSvc.id, OnboardingSvc.id, action)
    }
  }
}
