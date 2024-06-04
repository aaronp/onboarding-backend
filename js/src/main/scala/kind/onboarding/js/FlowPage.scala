package kind.onboarding.js

import kind.logic._

import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
case class FlowPage(services: Services) {

  def markdown(): String = services.telemetry.asMermaid().execOrThrow()

}
