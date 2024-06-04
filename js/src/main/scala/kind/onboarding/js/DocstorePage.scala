package kind.onboarding.js

import kind.logic._

import scala.scalajs.js.JSConverters._
import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
case class DocstorePage(services: Services) {
  def jason() = {
    services.asTree.map(_.collapse).runAsUJson.asJSON
  }

  /** @param newName
    *   the database name
    * @return
    *   a tuple represented as a JS array (formatted and db)
    */
  def changeDbName(newName: String) = {
    val db = Services.readDatabase(newName).getOrElse(services.asTree.execOrThrow())
    Seq(db.formatted, db.asJson.render(2)).toJSArray
  }

  def formatted() = {
    services.asTree.map(_.formatted).execOrThrow()
  }
}
