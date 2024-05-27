package kind.onboarding.js

import kind.logic.*
import kind.logic.json.PathTree
import kind.logic.telemetry.*
import kind.onboarding.docstore.*
import kind.onboarding.docstore.model.SaveDocument200Response
import org.scalajs.dom
import upickle.default.*

import scala.scalajs.js.JSConverters.*
import scala.scalajs.js.annotation.*
import scala.util.*
import scala.util.control.NonFatal

/** These are the 'convenience' functions made available to the front-end via 'createNewService'
  */
@JSExportAll
case class Services(
    docStore: DocStoreApp,
    database: DocStoreHandler.InMemory,
    telemetry: Telemetry
) {

  private def databaseDump(): PathTree = database.asTree.execOrThrow()

  /** Saves the database at the given name
    * @param name
    *   the name
    */
  def saveDatabaseAs(name: String) = {
    val dump = databaseDump()
    val value = write(dump)
    dom.window.localStorage.setItem(name, value)
  }

  def snapshotDatabase() = saveDatabaseAs("default")

  def listUsers() = docStore.listChildren("users").toJSArray

  def createNewUser(json: String): Json = {
    json.as[User] match {
      case Failure(err) =>
        ujson.Obj(
          "success" -> false,
          "message" ->
            s"Error parsing json as user: >${json}<"
        )
      case Success(user) =>
        val id = user.name
        Try(docStore.saveDocument(s"users/${id}", user.asJson)) match {
          case Success(SaveDocument200Response(msg)) =>
            ujson.Obj(
              "success" -> true,
              "message" ->
                msg.getOrElse(s"Created user: $id")
            )
          case Failure(saveErr) =>
            ujson.Obj(
              "success" -> false,
              "message" ->
                s"Error saving: $saveErr"
            )
        }
    }
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
    val docStore = readDatabase("default") match {
      case Some(db) => DocStoreHandler(db)
      case None     => DocStoreHandler()
    }

    val telemetry = Telemetry()

    val docStoreApi = DocStoreApp(docStore)(using telemetry)
    Services(docStoreApi, docStore, telemetry)
  }
}
