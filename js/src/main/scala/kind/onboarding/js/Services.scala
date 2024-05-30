package kind.onboarding.js

import kind.logic.*
import kind.logic.json.PathTree
import kind.logic.telemetry.*
import kind.onboarding.bff.*
import org.scalajs.dom
import upickle.default.*
import kind.onboarding.refdata.*
import scala.scalajs.js
import scala.scalajs.js.annotation.*
import scala.util.*
import scala.util.control.NonFatal
import kind.onboarding.bff.BackendForFrontend
import zio.*
import kind.onboarding.auth.User
import scala.scalajs.js.JSConverters.*

/** These are the 'convenience' functions made available to the front-end via 'createNewService'
  */
@JSExportAll
case class Services(database: Ref[PathTree], bff: BackendForFrontend, telemetry: Telemetry) {

  def asTree = database.get

  /** @return
    *   the scripts for the operations category page
    */
  def newCategoryPage() = CategoryPage(this)

  def newDocstorePage() = DocstorePage(this)

  def saveCategories(data: js.Dynamic) =
    data.runWithJsonAs[Seq[Category], ActionResult](bff.saveCategories)

  def listCategories() = bff.listCategories().getAsJS { c =>
    c.map(_.asJSON).toJSArray
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

  def listUsers() = bff.listUsers().getAsJS(_.toJSArray)

  def getUser(name: String) = bff.getUser(name).getAsJS {
    case Some(found) => found.asJSON
    case None        => ActionResult.fail("User not found").asJSON
  }

  def createNewUser(json: String): ActionResult =
    json.runWithJsonAs[User, ActionResult](bff.createNewUser)
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
