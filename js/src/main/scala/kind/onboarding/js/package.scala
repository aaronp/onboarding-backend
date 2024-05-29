package kind.onboarding

import ujson.Value.Value
import upickle.default.*

import kind.logic.json.mergeWith
import scala.scalajs.js
import scala.scalajs.js.JSON
import util.Try

package object js {
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

    /** Convenience method for merging these values as javascript JSON (rather than ujson)
      */
    def mergeAsJSON[B: ReadWriter](other: B): js.Dynamic = merge(other).asJavascriptObject

    /** Combines this object with another object as a json value
      * @param other
      *   the other object
      * @tparam B
      *   the other type
      * @return
      *   the merged json value
      */
    def merge[B: ReadWriter](other: B): Value = asUJson.mergeWith(other.asUJson)

  }
  extension (json: ujson.Value) {
    def asJavascriptObject: js.Dynamic = JSON.parse(json.render(0))
  }
}
