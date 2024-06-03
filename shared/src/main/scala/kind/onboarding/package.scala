package kind
import kind.logic.*
import upickle.default.*
import util.Try
package object onboarding {

  extension (data: Json) {
    def as[A: ReadWriter]: Try[A] = Try(read[A](data))
  }
}
