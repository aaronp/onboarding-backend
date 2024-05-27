package kind.onboarding.docstore

import kind.logic.*
import kind.logic.telemetry.*
import kind.onboarding.docstore.model.*
import kind.onboarding.docstore.api.*

trait DocStoreApp extends DefaultService

object DocStoreApp {

  val Id = Actor.service[DocStoreApp]

  def apply(
      impl: DocStoreHandler = DocStoreHandler.apply()
  )(using telemetry: Telemetry): DocStoreApp = {
    val logic: [A] => DocStoreLogic[A] => Result[A] = impl.defaultProgram
    App(logic)
  }

  /** The 'App' provides an implementation of the service API which defines the implementations in
    * terms of a partial function
    *
    * @param originalLogic
    *   the handler logic
    * @param telemetry
    *   the telemetry used to track calls
    */
  class App(originalLogic: [A] => DocStoreLogic[A] => Result[A])(using telemetry: Telemetry)
      extends RunnableProgram[DocStoreLogic](originalLogic)
      with DocStoreApp {
    override protected def appCoords = Id

    def withOverride(overrideFn: PartialFunction[DocStoreLogic[?], Result[?]]): App = {
      val newLogic: [A] => DocStoreLogic[A] => Result[A] = [A] => {
        (_: DocStoreLogic[A]) match {
          case value if overrideFn.isDefinedAt(value) =>
            overrideFn(value).asInstanceOf[Result[A]]
          case value => logic(value).asInstanceOf[Result[A]]
        }
      }
      App(newLogic)(using telemetry)
    }

    def compareDocuments(
        compareDocumentsRequest: CompareDocumentsRequest
    ) = run(DocStoreLogic.diffPaths(compareDocumentsRequest)).execOrThrow()

    def copyDocument(
        copyDocumentRequest: CopyDocumentRequest
    ) = run(DocStoreLogic.copyPaths(copyDocumentRequest)).execOrThrow()

    override def deleteDocument(path: String) = run(DocStoreLogic.delete(path)).execOrThrow()

    override def getDocument(
        path: String,
        version: Option[String]
    ): Json | GetDocument404Response = {
      run(DocStoreLogic.get(path, version)).execOrThrow()
    }

    override def listChildren(path: String): List[String] =
      run(DocStoreLogic.listChildren(path))
        .execOrThrow()
        .filter(_.trim.nonEmpty) // hack - we should fix '.asPath'

    override def getMetadata(path: String) = run(DocStoreLogic.metadata(path)).execOrThrow()

    override def saveDocument(path: String, body: Json): SaveDocument200Response = {
      run(DocStoreLogic.save(path, body)).execOrThrow()
    }
    override def updateDocument(
        path: String,
        body: Json
    ) = run(DocStoreLogic.update(path, body)).execOrThrow()
  }

}
