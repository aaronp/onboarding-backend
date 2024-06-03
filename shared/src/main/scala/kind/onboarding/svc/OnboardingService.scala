package kind.onboarding.svc

import kind.onboarding.*
import kind.onboarding.docstore.model.*
import kind.logic.*
import kind.logic.telemetry.Telemetry
import kind.onboarding.docstore.DocStoreApp
import kind.logic.ActionResult
import zio.*
import util.*
import Systems.*
import upickle.default.*

trait OnboardingService {
  def saveDoc(data: Json): Task[DocId | ActionResult]
  def getDoc(id: DocId): Task[Option[Json]]
  def listDocs(): Task[Seq[Json]]
  def approve(id: DocId): Task[Option[Json] | ActionResult]
}

object OnboardingService {

  case class Impl(docStore: DocStoreApp)(using telemetry: Telemetry) extends OnboardingService {

    override def saveDoc(data: Json): Task[DocId | ActionResult] = {
      val action = data.withKey("input").merge("saveDoc".withKey("action"))
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
                .upsertDocumentVersioned(s"drafts/${id}", data)
                .asTaskTraced(OnboardingSvc.id, DB.id, action)
                .as(id)
          }

        case Failure(err) =>
          ActionResult
            .fail(s"Failed to parse document: ${err.getMessage}")
            .asTaskTraced(OnboardingSvc.id, OnboardingSvc.id, action)
      }
    }

    override def listDocs(): Task[Seq[Json]] = {
      val action = "listDocs".withKey("action")
      docStore.query("drafts", None).asTaskTraced(OnboardingSvc.id, DB.id, action)
    }

    override def getDoc(id: DocId): Task[Option[Json]] = {
      val action = id.withKey("input").merge("getDoc".withKey("action"))
      val found: Option[Json] = docStore.getDocumentLatest(s"drafts/${id}") match {
        case GetDocument404Response(_) => None
        case data: Json                => Some(data)
      }
      found.asTaskTraced(OnboardingSvc.id, DB.id, action)
    }

    override def approve(id: DocId) = {
      getDoc(id).flatMap {
        case Some(data) =>
          val action = id.withKey("input").merge("approve".withKey("action"))

          data.as[DraftDoc] match {
            case Success(draft) =>
              docStore
                .upsertDocumentVersioned(s"approved/${id}", draft.approve(true).asUJson)
                .asTaskTraced(OnboardingSvc.id, DB.id, action)
                .map(Some(_))
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

  def apply(docStore: DocStoreApp)(using telemetry: Telemetry) = Impl(docStore)

}
