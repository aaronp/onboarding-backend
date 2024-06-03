package kind.onboarding.js

import kind.logic.*
import scala.scalajs.js.annotation.JSExportAll
import scala.scalajs.js.JSConverters._

@JSExportAll
case class OnboardingPage(services: Services) {

  def subCategoryOptions(name: JS) = {
    services.bff.getCategory(s"$name").execOrThrow() match {
      case Some(found) => found.subCategories.toSeq.sorted.toJSArray
      case None        => Seq.empty[String].toJSArray
    }
  }
}
