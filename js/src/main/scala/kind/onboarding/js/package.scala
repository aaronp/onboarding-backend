package kind.onboarding

import ujson.Value.Value
import upickle.default.*

import scala.scalajs.js.JSON
import util.Try

package object js {
  extension (jason: String) {
    def asJsonValue: Try[Value]   = Try(ujson.read(jason))
    def as[A: ReadWriter]: Try[A] = Try(read[A](jason))
  }

  extension (json: ujson.Value) {
    def asJavascriptObject = JSON.parse(json.render(0))
  }
}
