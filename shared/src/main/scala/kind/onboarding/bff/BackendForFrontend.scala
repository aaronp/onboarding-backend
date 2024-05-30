package kind.onboarding.bff

import kind.onboarding.docstore.model.*
import kind.logic.*
import kind.logic.telemetry.*
import kind.onboarding.docstore.DocStoreApp
import kind.onboarding.refdata.{Category, CategoryAdminService, CategoryService}
import zio.*
import scala.util.*

trait BackendForFrontend {

  // === Users / AUTH ===
  def createNewUser(user: User): Task[Json]

  def getUser(id: String): Task[Json]

  def listUsers(): Task[Json]

  // === Category ref data ===
  def getCategory(name: String): Task[Json]

  def saveCategories(categories: Seq[Category]): Task[Json]

  def updateCategory(category: Category): Task[Json]
}

object BackendForFrontend {

  val Id   = Actor.service("onboarding", "bff")
  val DB   = Id.withName("database")
  val Auth = Id.withName("auth")

  def emptyJson = Map[String, String]().asUJson

  case class Impl(
      categoryRefData: CategoryService,
      categoryAdmin: CategoryAdminService,
      docStore: DocStoreApp
  )(using telemetry: Telemetry)
      extends BackendForFrontend {

    override def createNewUser(user: User) = {
      val id = user.name
      docStore.saveDocument(s"users/${id}", user.asUJson).asTaskTraced(Id, Auth, user).map {
        case SaveDocument200Response(msg) =>
          ActionResult(msg.getOrElse(s"Created user: $id")).asUJson
      }
    }
    override def listUsers() =
      docStore.listChildren("users").asTaskTraced(Id, Auth, ()).map(_.asUJson)
    override def getUser(id: String) = {
      docStore.getDocument(s"users/$id", None).asTaskTraced(Id, Auth, id).map {
        case found: ujson.Value => found
        case other              => ujson.Null
      }
    }

    override def saveCategories(categories: Seq[Category]): Task[Json] =
      categoryAdmin.set(categories).map { _ => ActionResult("saved").withData(emptyJson).asUJson }

    override def updateCategory(category: Category): Task[Json] =
      categoryAdmin.update(category).map { _ =>
        ActionResult("updated").withData(emptyJson).asUJson
      }

    override def getCategory(name: String) = {
      categoryRefData.getCategory(name).map(_.fold(emptyJson)(_.asUJson))
    }
  }

  def apply(docStore: DocStoreApp)(using telemetry: Telemetry): BackendForFrontend = {
    val category = Categories(docStore)
    new Impl(category, category, docStore)
  }
}
