package kind.onboarding.refdata

import upickle.default.*
case class Product(name: String, subTypes: Set[String]) derives ReadWriter {
  def asJson = writeJs(this)
}
