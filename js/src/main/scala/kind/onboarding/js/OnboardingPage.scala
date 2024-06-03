package kind.onboarding.js

import kind.onboarding.refdata._
import kind.onboarding.bff._
import kind.logic.*

import scala.scalajs.js.annotation.JSExportAll
import scala.scalajs.js.JSConverters._
import util.*

@JSExportAll
case class OnboardingPage(services: Services) {

  def categoryOptions() = {
    services.bff
      .listCategories()
      .execOrThrow()
      .map { case Category(name, _) =>
        LabeledValue(name, name).asJSON
      }
      .toJSArray
  }

  def onSaveDraft(draft: JS) = {
    println(s"onSaveDraft: ${draft.asJsonString}")

    draft.jsonAsUJson match {
      case Failure(err) =>
        ActionResult.fail(s"Error parsing json: >${draft.asJsonString}<")
      case Success(jason) =>
        services.bff
          .saveDraft(jason)
          .execOrThrow() match {
          case id: String           => ActionResult("ok").withData(id).asJSON
          case result: ActionResult => result.asJSON
        }
    }
  }
  def subCategoryOptions(category: JS) = {
    services.bff.getCategory(category.toString).execOrThrow() match {
      case Some(Category(_, subCategories)) =>
        subCategories.map { name =>
          LabeledValue(name).asJSON
        }.toJSArray
      case None => List(LabeledValue(s"No subcategories found for ${category}").asJSON).toJSArray
    }
  }
}
