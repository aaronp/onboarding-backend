package kind.onboarding

import ujson.Value.Value
import upickle.default.*
import util.Try

package object js {
  extension (jason: String) {
    def asJsonValue: Try[Value]   = Try(ujson.read(jason))
    def as[A: ReadWriter]: Try[A] = Try(read[A](jason))
  }
}
