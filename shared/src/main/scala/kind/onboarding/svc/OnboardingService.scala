package kind.onboarding.svc

import kind.logic._
import kind.logic.json._
import kind.logic.telemetry.Telemetry
import kind.onboarding._
import kind.onboarding.docstore.DocStoreApp
import kind.onboarding.docstore.model._
import ujson.Value
import upickle.default._
import zio._

import Systems._

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

    override def saveDraft(data: Json): Task[DocId | ActionResult] = SaveDraft(docStore, data)

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

    override def withdraw(id: DocId, withdrawn: Boolean) =
      WithdrawDraft(docStore, id, getDraft(id), withdrawn)

    override def approve(id: DocId, approved: Boolean) =
      ApproveDraft(docStore, id, getDraft(id), approved)
  }
}
