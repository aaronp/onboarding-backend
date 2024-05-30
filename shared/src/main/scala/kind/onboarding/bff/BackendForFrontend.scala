package kind.onboarding.bff

import kind.logic.*
import kind.onboarding.docstore.DocStoreApp
import kind.onboarding.refdata.{Category, CategoryAdminService, CategoryService}
import zio.ZIO

trait BackendForFrontend {}

object BackendForFrontend {

  case class Impl(
      categoryRefData: CategoryService,
      categoryAdmin: CategoryAdminService,
      docStore: DocStoreApp,
      telemetry: Telemetry
  ) extends BackendForFrontend {

    /** @param name
      *   the category name
      * @return
      *   the category for the given name, or an empty json object
      */
    def getCategory(name: String): Task[Json] =
      categoryRefData.getCategory(name).map(_.fold(emptyJson)(_.asUJson))

    /** @param name
      *   the category name
      * @return
      *   a new category with the given name
      */
    def addCategory(name: String): Json = {
      val category = Category(name)
      categoryAdmin.add(category).asTry() match {
        case Success(_) => ActionResult("saved").withData(category)
        case Failure(err) =>
          ActionResult
            .fail(s"Error creating new project $name: $err", err.getMessage)
            .withData(emptyJson)
      }
    }

    def saveProduct(data: js.Dynamic) = {
      data.as[Category] match {
        case Success(newValue) =>
          products.update(newValue).asTry() match {
            case Success(_) =>
              newValue.withKey("product").mergeAsJSON(ActionResult("saved").withKey("result"))
            case Failure(err) =>
              ActionResult.fail(s"Error saving: $err", err.getMessage).withKey("result").asJSON
          }
        case Failure(err) =>
          ActionResult
            .fail(s"error parsing data: >${data.asJsonString}< : $err")
            .withKey("result")
            .asJSON
      }
    }

    def saveProducts(data: js.Dynamic) = {
      data.as[Seq[Category]] match {
        case Success(newValue) =>
          Try(products.set(newValue).execOrThrow()) match {
            case Success(_)   => ActionResult("saved").asJSON
            case Failure(err) => ActionResult.fail(s"Error saving: $err", err.getMessage).asJSON
          }
        case Failure(err) =>
          ActionResult.fail(s"error parsing data: >${data.asJsonString}< : $err").asJSON
      }
    }

    def listProducts(): Seq[js.Dynamic] =
      products
        .products()
        .execOrThrow()
        .map { case product @ Category(name, _) =>
          product.mergeAsJSON(LabeledValue(name))
        }

  }

  def apply(docStore: DocStoreApp, telemetry: Telemetry): BackendForFrontend = {
    val category = Categories(docStore, telemetry)
    new Impl(category, category, docStore, telemetry)
  }
}
