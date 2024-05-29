package kind.onboarding

import ujson.Value.Value
import upickle.default.*

import scala.scalajs.js
import scala.scalajs.js.JSON
import util.Try

package object js {
  extension (jason: String) {
    def asJsonValue: Try[Value]   = Try(ujson.read(jason))
    def as[A: ReadWriter]: Try[A] = Try(read[A](jason))
  }

  extension [A: ReadWriter](value: A) {
    def asJSON: js.Dynamic = write(value).asJavascriptObject
  }
  extension (json: ujson.Value) {
    def asJavascriptObject: js.Dynamic = JSON.parse(json.render(0))
  }
}
