package kind.onboarding.refdata

import kind.logic.telemetry._
import kind.onboarding.docstore.DocStoreApp
import zio.Task
import zio._

object Categories {

  def apply(docStore: DocStoreApp)(using telemetry: Telemetry): CategoryService &
    CategoryAdminService = {
    Delegate(CategoryService(docStore), CategoryAdminService(docStore))
  }

  def inMemory(): Task[CategoryService & CategoryAdminService] = for {
    reader <- CategoryService.inMemory
    writer = CategoryAdminService(reader.db)
  } yield Delegate(reader, writer)

}
