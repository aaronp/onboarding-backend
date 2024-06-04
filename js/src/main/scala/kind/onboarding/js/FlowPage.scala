package kind.onboarding.js

import kind.logic._

import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
case class FlowPage(services: Services) {

  def markdown(): String = {
    val calls = services.telemetry.calls.execOrThrow()
    if (calls.isEmpty) {
      "No calls found"
    } else {
      services.telemetry.asMermaid().execOrThrow()
    }
  }

}
