package kind.onboarding.js

import upickle.default.*

case class LabeledValue(label: String, value: String) derives ReadWriter
object LabeledValue {
  def apply(value: String): LabeledValue = LabeledValue(value, value)
}
