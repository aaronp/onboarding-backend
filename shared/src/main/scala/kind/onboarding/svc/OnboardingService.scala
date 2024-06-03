package kind.onboarding.svc

import kind.onboarding.*
import kind.onboarding.docstore.model.*
import kind.logic.*
import kind.logic.{as => jsonAs}
import kind.logic.json.*
import kind.logic.telemetry.Telemetry
import kind.onboarding.docstore.DocStoreApp
import zio.*
import util.*
import Systems.*
import upickle.default.*
import ujson.Value

trait OnboardingService {
  def saveDraft(data: Json): Task[DocId | ActionResult]
  def getDraft(id: DocId): Task[Option[Json]]
  def getApprovedDoc(id: DocId): Task[Option[Json]]
  def listDrafts(): Task[Seq[Json]]
  def listDraftsForUser(userId: String): Task[Seq[Json]]
  def listApprovedDocs(): Task[Seq[Json]]
  def approve(id: DocId, approved: Boolean): Task[Option[Json] | ActionResult]
  def withdraw(id: DocId, withdrawn: Boolean): Task[Option[Json] | ActionResult]
}

object OnboardingService {
  def apply(docStore: DocStoreApp)(using telemetry: Telemetry): OnboardingService = Impl(docStore)

  trait Delegate(onboardingService: OnboardingService) extends OnboardingService {
    override def saveDraft(data: Json): Task[DocId | ActionResult] =
      onboardingService.saveDraft(data)
    override def getDraft(id: DocId): Task[Option[Json]] = onboardingService.getDraft(id)
    override def getApprovedDoc(id: DocId): Task[Option[Json]] =
      onboardingService.getApprovedDoc(id)
    override def listDrafts(): Task[Seq[Json]]       = onboardingService.listDrafts()
    override def listDraftsForUser(userId: String)   = onboardingService.listDraftsForUser(userId)
    override def listApprovedDocs(): Task[Seq[Json]] = onboardingService.listApprovedDocs()
    override def approve(id: DocId, approved: Boolean): Task[Option[Json] | ActionResult] =
      onboardingService.approve(id, approved)
    override def withdraw(id: DocId, withdrawn: Boolean): Task[Option[Json] | ActionResult] =
      onboardingService.withdraw(id, withdrawn)
  }

  private class Impl(docStore: DocStoreApp)(using telemetry: Telemetry) extends OnboardingService {

    override def saveDraft(data: Json): Task[DocId | ActionResult] = {
      val action = data.withKey("input").merge("saveDraft".withKey("action"))
      data.jsonAs[DraftDoc] match {
        case Success(doc) =>
          val id = asId(doc.name)

          // check so we don't naughtily approve draft docs here
          data.jsonAs[ApprovedDoc] match {
            case Success(alreadyApproved) if alreadyApproved.approved =>
              ActionResult
                .fail(s"Rejected as doc '${id}' is already approved: ${data.render(2)}")
                .asTaskTraced(OnboardingSvc.id, DB.id, action)
            case _ =>
              // good - there sholdn't be an 'approved' field at this stage
              docStore
                .upsertDocumentVersioned(s"docs/drafts/${id}", data)
                .asTaskTraced(OnboardingSvc.id, DB.id, action)
                .as(id)
          }

        case Failure(err) =>
          ActionResult
            .fail(s"Failed to parse document: ${err.getMessage}")
            .asTaskTraced(OnboardingSvc.id, OnboardingSvc.id, action)
      }
    }

    private def getLatestVersionForNode(doc: PathTree) = {
      val versions = doc.keys.map {
        case s"v${version}" => version.toInt
        case other =>
          sys.error(s"Data Corruption: unexpected versioned child $other")
      }
      versions.toSeq.sorted.lastOption match {
        case Some(version) => doc.children.get(s"v${version}")
        case None          => None
      }
    }

    override def listApprovedDocs(): Task[Seq[Json]] = {
      val action = "listApprovedDocs".withKey("action")
      docStore.getNode("docs/approved").asTaskTraced(OnboardingSvc.id, DB.id, action).map {
        case None => Nil
        case Some(approvedNode) =>
          approvedNode.children.flatMap { case (id, draftNodeWithLatestChildren) =>
            val latest = getLatestVersionForNode(draftNodeWithLatestChildren)
            latest.map(_.merge(id.withKey("_id")))
          }.toSeq
      }
    }

    override def listDraftsForUser(userId: String) = {
      val action = "listDrafts".withKey("action").merge(userId.withKey("input"))
      listDrafts()
        .map { docs =>
          docs.flatMap { doc =>
            doc("data")
              .as[DraftDoc]
              .toOption
              .filter { draftDoc =>
                draftDoc.ownerUserId == userId
              }
              .map(_ => doc)
          }
        }
        .traceWith(OnboardingSvc.id, OnboardingSvc.id, action)
    }

    override def listDrafts(): Task[Seq[Json]] = {
      val action = "listDrafts".withKey("action")
      docStore.getNode("docs/drafts").asTaskTraced(OnboardingSvc.id, DB.id, action).map {
        case None => Nil
        case Some(draftsNode) =>
          draftsNode.children.flatMap { case (id, draftNodeWithLatestChildren) =>
            val latest = getLatestVersionForNode(draftNodeWithLatestChildren)
            latest.map(_.merge(id.withKey("_id")))
          }.toSeq
      }
    }

    override def getApprovedDoc(id: DocId): Task[Option[Json]] = {
      val action = id.withKey("input").merge("getApprovedDoc".withKey("action"))
      val found: Option[Json] = docStore.getDocumentLatest(s"docs/approved/${id}") match {
        case GetDocument404Response(_) => None
        case data: Json                => Some(data)
      }
      found.asTaskTraced(OnboardingSvc.id, DB.id, action)
    }

    override def getDraft(id: DocId): Task[Option[Json]] = {
      val action = id.withKey("input").merge("getDraft".withKey("action"))
      val found: Option[Json] = docStore.getDocumentLatest(s"docs/drafts/${id}") match {
        case GetDocument404Response(_) => None
        case data: Json                => Some(data)
      }
      found.asTaskTraced(OnboardingSvc.id, DB.id, action)
    }

    override def withdraw(id: DocId, withdrawn: Boolean) = {
      val action: Json = {
        id.withKey("id")                         //
          .merge(withdrawn.withKey("withdrawn")) //
          .merge("approve".withKey("action"))    //
      }

      getDraft(id).flatMap {
        case Some(data) =>
          data.jsonAs[DraftDoc] match {
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

    override def approve(id: DocId, approved: Boolean) = {
      val action =
        id.withKey("id").merge(approved.withKey("approved")).merge("withdraw".withKey("action"))

      getDraft(id).flatMap {
        case Some(data) =>
          def updateDraft(draft: DraftDoc): String = docStore.upsertDocumentVersioned(
            s"docs/drafts/${id}",
            data.merge(draft.approve(approved).asUJson)
          )

          data.jsonAs[DraftDoc] match {
            case Success(draft) if !approved =>
              // update the draft as rejected
              updateDraft(draft)
                .asTaskTraced(OnboardingSvc.id, DB.id, action)
                .map(_ => Option.apply(data))
            case Success(draft) =>
              // update both the draft and 'approved' documents
              {
                updateDraft(draft)
                val approvedDoc = data.merge(draft.approve(approved).asUJson)
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
}
