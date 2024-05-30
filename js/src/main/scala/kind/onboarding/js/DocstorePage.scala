package kind.onboarding.js

import kind.logic._
import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
case class DocstorePage(services: Services) {
  def jason() = {
    val result = services.asTree.map(_.collapse).runAsUJson
    println(result.render(2))
    result.asJSON
  }
  def full() = {
    val result = services.asTree.map(_.asUJson).execOrThrow()
    println(result.render(2))
    result.asJSON
  }
  def formatted() = {
    services.asTree.map(_.formatted).execOrThrow()
  }
}
