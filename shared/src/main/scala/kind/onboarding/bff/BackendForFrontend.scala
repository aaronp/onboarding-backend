package kind.onboarding.bff

import kind.onboarding.Systems.*
import kind.onboarding.docstore.model.*
import kind.logic.*
import kind.logic.telemetry.*
import kind.onboarding.docstore.DocStoreApp
import kind.onboarding.refdata.{Category, CategoryAdminService, CategoryService}
import zio.*
import kind.onboarding.refdata.Categories
import kind.onboarding.auth.User

/** Representation of a single point of entry for our front-end
  */
trait BackendForFrontend {

  // === Users / AUTH ===
  def createNewUser(user: User): Task[Json]

  def getUser(id: String): Task[Json]

  def listUsers(): Task[Json]

  // === Category ref data ===
  def listCategories(): Task[Json]

  def getCategory(name: String): Task[Json]

  def addCategory(name: String): Task[Json]

  def saveCategories(categories: Seq[Category]): Task[Json]

  def updateCategory(category: Category): Task[Json]
}

object BackendForFrontend {

  def emptyJson = Map[String, String]().asUJson

  case class Impl(
      categoryRefData: CategoryService,
      categoryAdmin: CategoryAdminService,
      docStore: DocStoreApp
  )(using telemetry: Telemetry)
      extends BackendForFrontend {

    override def createNewUser(user: User) = {
      val id = user.name
      docStore
        .saveDocument(s"users/${id}", user.asUJson)
        .asTaskTraced(BFF.id, Auth.id, user)
        .map { case SaveDocument200Response(msg) =>
          ActionResult(msg.getOrElse(s"Created user: $id")).asUJson
        }
    }
    override def listUsers() =
      docStore.listChildren("users").asTaskTraced(BFF.id, Auth.id, ()).map(_.asUJson)

    override def getUser(id: String) = {
      docStore.getDocument(s"users/$id", None).asTaskTraced(BFF.id, Auth.id, id).map {
        case found: ujson.Value => found
        case other              => ujson.Null
      }
    }

    override def listCategories(): Task[Json] =
      categoryRefData
        .categories()
        .map { list =>
          list.map { case Category(name, _) =>
            LabeledValue(name)
          }.asUJson
        }
        .traceWith(BFF.id, CategoryRead.id)

    override def getCategory(name: String) = {
      categoryRefData
        .getCategory(name)
        .traceWith(BFF.id, CategoryRead.id, name)
        .map(_.fold(emptyJson)(_.asUJson))
    }

    override def addCategory(name: String): Task[Json] = {
      val category = Category(name)
      categoryAdmin.add(category).traceWith(BFF.id, CategoryAdmin.id, name).map { _ =>
        ActionResult("saved").withData(category).asUJson
      }
    }

    override def saveCategories(categories: Seq[Category]): Task[Json] =
      categoryAdmin.set(categories).traceWith(BFF.id, CategoryAdmin.id, categories).map { _ =>
        ActionResult("saved").withData(emptyJson).asUJson
      }

    override def updateCategory(category: Category): Task[Json] =
      categoryAdmin.update(category).traceWith(BFF.id, CategoryAdmin.id, category).map { _ =>
        ActionResult("updated").withData(emptyJson).asUJson
      }
  }

  def apply(docStore: DocStoreApp)(using telemetry: Telemetry): BackendForFrontend = {
    val category = Categories(docStore)
    new Impl(category, category, docStore)
  }
}
