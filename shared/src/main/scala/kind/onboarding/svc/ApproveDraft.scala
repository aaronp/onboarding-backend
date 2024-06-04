package kind.onboarding.svc

import kind.logic._
import kind.logic.telemetry.Telemetry
import kind.onboarding._
import kind.onboarding.docstore.DocStoreApp
import ujson.Value
import upickle.default._
import zio._

import util._
import Systems._

private[svc] object ApproveDraft {
  def apply(docStore: DocStoreApp, id: DocId, getDraft: Task[Option[Json]], approved: Boolean)(using
      telemetry: Telemetry
  ) = {
    val action =
      id.withKey("id").merge(approved.withKey("approved")).merge("withdraw".withKey("action"))

    getDraft.flatMap {
      case Some(data) =>
        def updateDraft(draft: DraftDoc): String = docStore.upsertDocumentVersioned(
          s"docs/drafts/${id}",
          data.merge(draft.approve(approved).asUJson.withTimestamp())
        )

        data.as[DraftDoc] match {
          case Success(draft) if !approved =>
            // update the draft as rejected
            updateDraft(draft)
              .asTaskTraced(OnboardingSvc.id, DB.id, action)
              .map(_ => Option.apply(data))
          case Success(draft) =>
            // update both the draft and 'approved' documents
            {
              updateDraft(draft)
              val approvedDoc = data.merge(draft.approve(approved).asUJson.withTimestamp())
              docStore.upsertDocumentVersioned(s"docs/approved/${id}", approvedDoc)
              Option(approvedDoc)
            }
              .asTaskTraced(OnboardingSvc.id, DB.id, action)

          case Failure(thisShouldntHappen) =>
            ActionResult
              .fail(
                s"Corrupted data found for doc '${id}'. Error is ${thisShouldntHappen} for data: ${data
                    .render(2)}"
              )
              .asTaskTraced(OnboardingSvc.id, OnboardingSvc.id, action)
        }
      case None =>
        Option
          .empty[Json]
          .asTaskTraced(
            OnboardingSvc.id,
            OnboardingSvc.id,
            id.withKey("input").merge("approve".withKey("action"))
          )
    }
  }
}
