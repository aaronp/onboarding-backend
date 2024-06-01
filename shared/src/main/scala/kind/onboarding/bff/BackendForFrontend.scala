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
import upickle.default.*

/** Representation of a single point of entry for our front-end
  */
trait BackendForFrontend {

  // === Users / AUTH ===
  def createNewUser(user: User): Task[ActionResult]

  def getUser(id: String): Task[Option[User]]

  def listUsers(): Task[Seq[String]]

  // === Category ref data ===
  def listCategories(): Task[Seq[Category]]

  def getCategory(name: String): Task[Option[Category]]

  def addCategory(name: String): Task[Category]

  def saveCategories(categories: Seq[Category]): Task[ActionResult]

  def updateCategory(category: Category): Task[Category]
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
        .asTaskTraced(BFF.id, DB.id, user.merge("createNewUser".withKey("action")))
        .map { case SaveDocument200Response(msg) =>
          ActionResult(msg.getOrElse(s"Created user: $id"))
        }
    }
    override def listUsers() =
      docStore
        .listChildren("users")
        .asTaskTraced(BFF.id, DB.id, "listUsers".withKey("action").asUJson)

    override def getUser(id: String) = {
      docStore
        .getDocument(s"users/$id", None)
        .asTaskTraced(BFF.id, DB.id, id.merge("getUser".withKey("action")))
        .map {
          case found: ujson.Value => Option(read[User](found))
          case other              => None
        }
    }

    override def listCategories() =
      categoryRefData
        .categories()
        .traceWith(BFF.id, CategoryRead.id, "listCategories".withKey("action").asUJson)

    override def getCategory(name: String) = {
      categoryRefData
        .getCategory(name)
        .traceWith(
          BFF.id,
          CategoryRead.id,
          name.withKey("name").merge("getCategory".withKey("action"))
        )
    }

    override def addCategory(name: String) = {
      val category = Category(name)
      val action   = name.withKey("name").merge("addCategory".withKey("action"))
      categoryAdmin.add(category).traceWith(BFF.id, CategoryAdmin.id, action).map { _ =>
        category
      }
    }

    override def saveCategories(categories: Seq[Category]) =
      val action = categories.withKey("categories").merge("saveCategories".withKey("action"))
      categoryAdmin.set(categories).traceWith(BFF.id, CategoryAdmin.id, action).map { _ =>
        ActionResult("saved")
      }

    override def updateCategory(category: Category) =
      val action = category.withKey("category").merge("updateCategory".withKey("action"))
      categoryAdmin.update(category).traceWith(BFF.id, CategoryAdmin.id, action).map { _ =>
        category
      }
  }

  def apply(docStore: DocStoreApp)(using telemetry: Telemetry): BackendForFrontend = {
    val category = Categories(docStore)
    new Impl(category, category, docStore)
  }
}
