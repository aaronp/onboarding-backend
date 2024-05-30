package kind.onboarding.bff

import kind.onboarding.docstore.DocStoreApp
import kind.logic.*
import kind.onboarding.refdata.{Category, CategoryAdminService, CategoryService}

import scala.scalajs.js
import scala.util.{Failure, Success, Try}
import zio.{Task, *}

object Categories {

  val Id = BackendForFrontend.Id.withName("categories")

  def apply(docStore: DocStoreApp): CategoryService & CategoryAdminService = {
    Impl(docStore)
  }

  def inMemory(): Task[CategoryService & CategoryAdminService] = for {
    reader <- CategoryService.inMemory
    writer = CategoryAdminService(reader.db)
  } yield Delegate(reader, writer)

  class Delegate(reader: CategoryService, writer: CategoryAdminService)
      extends CategoryService,
        CategoryAdminService {
    override def categories() = reader.categories()

    override def add(product: Category) = writer.add(product)

    override def update(product: Category) = writer.update(product)

    override def remove(product: String) = writer.remove(product)

    override def set(products: Seq[Category]) = writer.set(products)
  }

  class Impl(docStore: DocStoreApp) extends CategoryService, CategoryAdminService {
    override def categories(): Task[Seq[Category]] = {
      docStore.query("refdata/categories", None).???

    }

    override def add(product: Category) = ???

    override def update(product: Category) = ???

    override def remove(product: String) = ???

    override def set(products: Seq[Category]) = ???
  }
}
