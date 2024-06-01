package kind.onboarding.js

import scala.scalajs.js.annotation.JSExportAll
import kind.logic._

@JSExportAll
case class FlowPage(services: Services) {

  def markdown(): String = services.telemetry.asMermaid().execOrThrow()

}
