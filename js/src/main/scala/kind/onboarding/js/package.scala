package kind.onboarding

import ujson.Value.Value
import upickle.default.*

import kind.logic.json.*
import kind.logic.*
import scala.scalajs.js
import scala.scalajs.js.JSON
import util.*
import zio.*
import scala.scalajs.js.annotation.JSExportAll

package object js {

  private def emptyJson = Map[String, String]().asUJson

  type JS = js.Dynamic

  extension [A](task: Task[A]) {

    def getAsJS(using rw: ReadWriter[A])(f: A => js.Any): js.Any = {
      getOrActionResult match {
        case data: A                    => f(data)
        case actionResult: ActionResult => actionResult.asJSON
      }
    }

    def getOrActionResult: A | ActionResult = task.asTry() match {
      case Success(result) => result
      case Failure(err) =>
        println(s"getOrActionResult was err $err")
        ActionResult.fail(s"Error: $err", err.getMessage)
    }
  }
  extension (task: Task[Json]) {
    def runAsUJson: Json = task.asTry() match {
      case Success(result) => ActionResult("ok").withData(result)
      case Failure(err) =>
        ActionResult.fail(s"Error: $err", err.getMessage).withData(emptyJson)
    }
    def runAsJSON: js.Dynamic = runAsUJson.asJavascriptObject
  }

  extension (jason: String) {
    def asJsonValue: Try[Value]             = Try(ujson.read(jason))
    def jsonStringAs[A: ReadWriter]: Try[A] = Try(read[A](jason))

    /** postfix syntax to try and parse the json string as an instance of A, then using that value
      * as an input to the operation
      */
    def runWithJsonAs[A: ReadWriter, B](f: A => Task[B]): ActionResult | B = {
      jason.jsonStringAs[A] match {
        case Success(value) => f(value).getOrActionResult
        case Failure(err) =>
          ActionResult.fail(s"Error parsing json: >${jason}<")
      }
    }
  }

  extension (json: js.Dynamic) {
    def asJsonString: String      = JSON.stringify(json)
    def as[A: ReadWriter]: Try[A] = asJsonString.jsonStringAs[A]
    def runWithJsonAs[A: ReadWriter, B](f: A => Task[B]): B | ActionResult =
      asJsonString.runWithJsonAs[A, B](f)
  }

  extension [A: ReadWriter](value: A) {
    def asUJson            = writeJs(value)
    def asJSON: js.Dynamic = asUJson.asJavascriptObject
  }
  extension (json: ujson.Value) {
    def asJavascriptObject: js.Dynamic = JSON.parse(json.render(0))
  }
}
