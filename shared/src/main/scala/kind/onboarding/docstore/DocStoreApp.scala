package kind.onboarding.docstore

import kind.logic.*
import kind.logic.telemetry.*
import kind.onboarding.docstore.model.*
import kind.onboarding.docstore.api.*

trait DocStoreApp extends DefaultService {

  /** Save the document at the given path, where the previous version is still kept (the documents
    * are saved at sub-paths under the given path)
    *
    * NOTE: we could make this the default behaviour, or add it to the REST service
    */
  def saveDocumentVersioned(path: String, body: Json): String
  def updateDocumentVersioned(path: String, body: Json): String

  def getDocumentLatest(path: String): Json | GetDocument404Response

}

object DocStoreApp {

  val Id = Actor.service("onboarding", "DB")

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

    override def query(path: String, filter: Option[String]): List[ujson.Value] = {
      run(DocStoreLogic.query(path, filter)).execOrThrow()
    }
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

    override def getMetadata(path: String) = run(DocStoreLogic.metadata(path)).execOrThrow()

    /** Save the document at the given path, where the previous version is still kept (the documents
      * are saved at sub-paths under the given path)
      *
      * NOTE: we could make this the default behaviour, or add it to the REST service
      */
    override def saveDocumentVersioned(path: String, body: Json): String = {
      run {
        for {
          kids <- DocStoreLogic.listChildren(path)
          newPath = s"${chomp(path)}/${asVersion(kids.size)}"
          result <- DocStoreLogic.save(newPath, body)
        } yield newPath
      }.execOrThrow()
    }

    /** To write the 'update' (or patch) in terms of our other functions, we need to load the
      * latest, save that at the latest path, and then do an update at that latest version path
      * @param path
      *   the path to patch
      * @param body
      *   the data to update against the previous data
      * @return
      *   the path of the new document
      */
    override def updateDocumentVersioned(path: String, body: Json) = {
      getDocumentLatest(path) match {
        case data: Json =>
          val newPath = saveDocumentVersioned(path, data)
          updateDocument(newPath, body)
          newPath
        case notFound: GetDocument404Response => ""
      }
    }
    private def asVersion(size: Int) = s"v$size"
    private def chomp(path: String)  = if path.endsWith("/") then path.init else path

    override def getDocumentLatest(path: String): Json | GetDocument404Response = {
      run {
        for {
          kids   <- DocStoreLogic.listChildren(path)
          result <- DocStoreLogic.get(s"${chomp(path)}", kids.lastOption)
        } yield result
      }.execOrThrow()
    }

    override def saveDocument(path: String, body: Json): SaveDocument200Response = {
      run(DocStoreLogic.save(path, body)).execOrThrow()
    }
    override def updateDocument(
        path: String,
        body: Json
    ) = run(DocStoreLogic.update(path, body)).execOrThrow()
  }

}
