package kind.onboarding.js

import kind.logic._
import kind.onboarding.bff._
import kind.onboarding.refdata._

import scala.scalajs.js.JSConverters._
import scala.scalajs.js.annotation._

import util._

@JSExportAll
case class OnboardingPage(services: Services) {

  @JSExport("categoryOptions")
  def categoryOptions(): scala.scalajs.js.Array[JS] = {
    services.bff
      .listCategories()
      .execOrThrow()
      .map { case Category(name, _) =>
        LabeledValue(name, name).asJSON
      }
      .toJSArray
  }

  def loadProduct(draftId: String) = {
    services.bff.getDraft(draftId).execOrThrow() match {
      case Some(draft) => draft.asJSON
      case None        => ActionResult.fail(s"Draft not found: ${draftId}").asJSON
    }
  }

  def onSaveDraft(draft: JS) = {

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
