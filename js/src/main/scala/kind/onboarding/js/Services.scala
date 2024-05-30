package kind.onboarding.js

import kind.logic.*
import kind.logic.json.PathTree
import kind.logic.telemetry.*
import kind.onboarding.bff.*
import org.scalajs.dom
import upickle.default.*
import kind.onboarding.refdata.*
import scala.scalajs.js.JSON
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import scala.scalajs.js.annotation.*
import scala.util.*
import scala.util.control.NonFatal
import kind.onboarding.bff.BackendForFrontend
import zio.*

/** These are the 'convenience' functions made available to the front-end via 'createNewService'
  */
@JSExportAll
case class Services(database: Ref[PathTree], bff: BackendForFrontend, telemetry: Telemetry) {

  def asTree = database.get

  /** @param name
    *   the category name
    * @return
    *   the category for the given name, or an empty json object
    */
  def getCategory(name: String): Task[Json] =
    categoryRefData.getCategory(name).map(_.fold(emptyJson)(_.asUJson))

  /** @param name
    *   the category name
    * @return
    *   a new category with the given name
    */
  def addCategory(name: String): Json = {
    val category = Category(name)
    categoryAdmin.add(category).asTry() match {
      case Success(_) => ActionResult("saved").withData(category)
      case Failure(err) =>
        ActionResult
          .fail(s"Error creating new project $name: $err", err.getMessage)
          .withData(emptyJson)
    }
  }

  def saveCategory(data: js.Dynamic) = data.runWithInput[Category](bff.updateCategory)

  def saveCategories(data: js.Dynamic) = {
    data.runWithInput[Seq[Category]](bff.saveCategories)
  }

  def listProducts(): Seq[js.Dynamic] =
    products
      .products()
      .execOrThrow()
      .map { case product @ Category(name, _) =>
        product.mergeAsJSON(LabeledValue(name))
      }

  private def databaseDump(): PathTree = asTree.execOrThrow()

  /** Saves the database at the given name
    * @param name
    *   the name
    */
  def saveDatabaseAs(name: String) = {
    val dump  = databaseDump()
    val value = write(dump)
    dom.window.localStorage.setItem(name, value)
  }

  def snapshotDatabase() = saveDatabaseAs("default")

  def listUsers() = bff.listUsers().toJSArray

  def getUser(name: String) = docStore.getDocument(s"users/$name", None) match {
    case found: ujson.Value => found.asJavascriptObject
    case other              => ujson.Null.asJavascriptObject
  }

  def createNewUser(json: String) = {
    json.runWithInput[User](bff.createNewUser)
  }
}

object Services {

  def readDatabase(name: String): Option[PathTree] = {
    try {
      Option(dom.window.localStorage.getItem(name)).flatMap { value =>
        value.as[PathTree].toOption
      }
    } catch {
      case NonFatal(e) =>
        println(s"Error reading '$name': $e")
        None
    }
  }
  @JSExportTopLevel("createService")
  def createService(): Services = {
    // it's OK to know about our local backend stuff here
    // as we'll swap out a 'real' backend which has a REST client
    // behind a similar function
    import kind.onboarding.docstore.*
    val docStore: DocStoreHandler.InMemory = readDatabase("default") match {
      case Some(db) => DocStoreHandler(db)
      case None     => DocStoreHandler()
    }
    val telemetryInst            = Telemetry()
    val docStoreApi: DocStoreApp = DocStoreApp(docStore)(using telemetryInst)

    val bff = BackendForFrontend(docStoreApi)(using telemetryInst)

    new Services(docStore.ref, bff, telemetryInst)
  }
}
