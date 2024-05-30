package kind.onboarding.refdata

import kind.onboarding.docstore.DocStoreApp
import kind.logic.telemetry.*
import zio.{Task, *}

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
