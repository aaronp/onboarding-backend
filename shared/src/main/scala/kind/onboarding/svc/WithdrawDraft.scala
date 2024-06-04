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

private[svc] object WithdrawDraft {
  def apply(docStore: DocStoreApp, id: DocId, getDraft: Task[Option[Json]], withdrawn: Boolean)(
      using telemetry: Telemetry
  ) = {
    val action: Json = {
      id.withKey("id")                         //
        .merge(withdrawn.withKey("withdrawn")) //
        .merge("approve".withKey("action"))    //
    }

    getDraft.flatMap {
      case Some(data) =>
        data.as[DraftDoc] match {
          case Success(draft) =>
            // update the draft as rejected
            docStore
              .upsertDocumentVersioned(
                s"docs/drafts/${id}",
                data.merge(withdrawn.withKey("withdrawn"))
              )
              .asTaskTraced(OnboardingSvc.id, DB.id, action)
              .map(_ => Option.apply(data))
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
            id.withKey("input").merge("withdraw".withKey("action"))
          )
    }
  }
}
