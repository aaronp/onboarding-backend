package kind.onboarding

import ujson.Value.Value
import upickle.default.*

import kind.logic.json.*
import kind.logic.*
import scala.scalajs.js
import scala.scalajs.js.JSON
import util.*
import zio.*

package object js {

  private def emptyJson = Map[String, String]().asUJson

  extension (task: Task[Json]) {
    def runAsUJson: Json = task.asTry() match {
      case Success(result) => ActionResult("ok").withData(result)
      case Failure(err) =>
        ActionResult.fail(s"Error: $err", err.getMessage).withData(emptyJson)
    }
    def runAsJSON: js.Dynamic = runAsUJson.asJavascriptObject
  }

  extension (jason: String) {
    def asJsonValue: Try[Value]   = Try(ujson.read(jason))
    def as[A: ReadWriter]: Try[A] = Try(read[A](jason))
    def runWithInput[A: ReadWriter](f: A => Task[Json]): scala.scalajs.js.Dynamic = {
      val response = jason.as[A] match {
        case Success(value) => f(value).runAsUJson
        case Failure(err) =>
          ActionResult.fail(s"Error parsing json: >${jason}<").asUJson
      }
      response.asJavascriptObject
    }
  }

  extension (json: js.Dynamic) {
    def asJsonString              = JSON.stringify(json)
    def as[A: ReadWriter]: Try[A] = asJsonString.as[A]
    def runWithInput[A: ReadWriter](f: A => Task[Json]): scala.scalajs.js.Dynamic =
      asJsonString.runWithInput[A](f)
  }

  extension [A: ReadWriter](value: A) {
    def asUJson            = writeJs(value)
    def asJSON: js.Dynamic = write(value).asJavascriptObject
  }
  extension (json: ujson.Value) {
    def asJavascriptObject: js.Dynamic = JSON.parse(json.render(0))
  }
}
