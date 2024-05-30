package kind.onboarding.js

import kind.logic.telemetry.Telemetry
import kind.onboarding.docstore.{DocStoreApp, DocStoreHandler}

/** TODO - move the Services stuff to here to that's just an adapter shim
  * @param docStore
  * @param database
  * @param products
  * @param telemetry
  */
class BackendForFrontend(
    docStore: DocStoreApp,
    database: DocStoreHandler.InMemory,
    products: Products,
    telemetry: Telemetry
) {}

object BackendForFrontend {
  def apply() = {

    val docStore = readDatabase("default") match {
      case Some(db) => DocStoreHandler(db)
      case None     => DocStoreHandler()
    }

    val telemetry = Telemetry()

    val docStoreApi = DocStoreApp(docStore)(using telemetry)

    val products                = Products.inMemory().execOrThrow()
    val categories: CategoryBFF = ???

  }
}
