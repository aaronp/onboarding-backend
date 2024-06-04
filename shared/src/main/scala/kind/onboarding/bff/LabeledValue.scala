package kind.onboarding.bff

import upickle.default._

case class LabeledValue(label: String, value: String) derives ReadWriter
object LabeledValue {
  def apply(value: String): LabeledValue = LabeledValue(value, value)
}
