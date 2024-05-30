package kind.onboarding

import ujson.Value.Value
import upickle.default.*

import kind.logic.json.mergeWith
import scala.scalajs.js
import scala.scalajs.js.JSON
import util.Try

package object js {

  def emptyJSON = Map.empty[String, String].asJSON

  extension (jason: String) {
    def asJsonValue: Try[Value]   = Try(ujson.read(jason))
    def as[A: ReadWriter]: Try[A] = Try(read[A](jason))
  }

  extension (json: js.Dynamic) {
    def asJsonString              = JSON.stringify(json)
    def as[A: ReadWriter]: Try[A] = asJsonString.as[A]
  }

  extension [A: ReadWriter](value: A) {
    def asUJson            = writeJs(value)
    def asJSON: js.Dynamic = write(value).asJavascriptObject
  }
  extension (json: ujson.Value) {
    def asJavascriptObject: js.Dynamic = JSON.parse(json.render(0))
  }
}
