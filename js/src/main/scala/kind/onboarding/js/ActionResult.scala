package kind.onboarding.js

import upickle.default.*

case class ActionResult(message: String, success: Boolean, warning: String = "", error: String = "")
    derives ReadWriter

object ActionResult {
  def apply(msg: String)               = new ActionResult(msg, true)
  def fail(msg: String)                = new ActionResult(msg, false)
  def fail(msg: String, error: String) = new ActionResult(msg, false, warning = "", error = error)
}
