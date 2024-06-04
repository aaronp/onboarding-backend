package kind.onboarding.js

import kind.logic._
import kind.logic.json.PathTree
import kind.logic.telemetry._
import kind.onboarding.auth.User
import kind.onboarding.bff.BackendForFrontend
import kind.onboarding.bff._
import kind.onboarding.refdata._
import org.scalajs.dom
import upickle.default._
import zio._

import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.annotation._
import scala.util._
import scala.util.control.NonFatal

/** These are the 'convenience' functions made available to the front-end via 'createNewService'
  */
@JSExportAll
case class Services(
    databaseName: String,
    database: Ref[PathTree],
    bff: BackendForFrontend,
    telemetry: Telemetry
) {

  def asTree = database.get

  /** @return
    *   the scripts for the operations category page
    */
  def newCategoryPage() = CategoryPage(this)

  def newDocstorePage() = DocstorePage(this)

  def newFlowPage() = FlowPage(this)

  def newDashboardPage() = DashboardPage(this)

  def newOnboardingPage() = OnboardingPage(this)

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

  def currentDatabaseName() = databaseName
  def currentDatabase()     = LabeledValue(databaseName).asJSON

  def snapshotDatabase() = saveDatabaseAs("default")

  def listUsers() = bff.listUsers().getAsJS(_.toJSArray)

  def getUser(name: String) = bff.getUser(name).getAsJS {
    case Some(found) => found.asJSON
    case None        => ActionResult.fail("User not found").asJSON
  }

  def createNewUser(json: String): ActionResult =
    json.runWithJsonAs[User, ActionResult](bff.createNewUser)

  def listDatabaseKeys() = Services.listDatabaseKeys(databaseName)

  def reloadService(dbName: String): Services = Services.createServiceForName(dbName)
}

object Services {

  /** @return
    *   a list of Labeled Values (for use in a drop-down) for the database keys
    */
  def listDatabaseKeys(currentDb: String) = {
    val keys = (0 until dom.window.localStorage.length).flatMap { i =>
      val key = dom.window.localStorage.key(i)
      // only include the keys which can be unmarshalled as PathTrees (our data)
      readDatabase(key).filter(d => d.children.nonEmpty || !d.data.isNull).map { data =>
        if (key == currentDb) {
          LabeledValue(s"${key} (current)", key, key == currentDb).asJSON
        } else {
          LabeledValue(key).asJSON
        }
      }
    }
    keys.toJSArray
  }

  def readDatabase(name: String): Option[PathTree] = {
    try {
      Option(dom.window.localStorage.getItem(name)).flatMap { value =>
        value.jsonStringAs[PathTree].toOption
      }
    } catch {
      case NonFatal(e) =>
        println(s"Error reading '$name': $e")
        None
    }
  }

  @JSExportTopLevel("createService")
  def createService(): Services = createServiceForName("default")

  @JSExportTopLevel("createServiceForName")
  def createServiceForName(dbName: String): Services = {
    // it's OK to know about our local backend stuff here
    // as we'll swap out a 'real' backend which has a REST client
    // behind a similar function
    import kind.onboarding.docstore.*
    val docStore: DocStoreHandler.InMemory = readDatabase(dbName) match {
      case Some(db) => DocStoreHandler(db)
      case None     => DocStoreHandler()
    }
    val telemetryInst            = Telemetry()
    val docStoreApi: DocStoreApp = DocStoreApp(docStore)(using telemetryInst)

    val bff = BackendForFrontend(docStoreApi)(using telemetryInst)

    new Services(dbName, docStore.ref, bff, telemetryInst)
  }
}
