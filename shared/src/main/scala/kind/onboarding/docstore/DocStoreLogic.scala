package kind.onboarding.docstore

import kind.logic.*
import kind.onboarding.docstore.model.*
import ujson.Value

type Json = ujson.Value
enum DocStoreLogic[A]:
  case CompareDocuments(compareDocumentsRequest: CompareDocumentsRequest)
      extends DocStoreLogic[CompareDocuments200Response | CompareDocuments400Response]
  case CopyDocument(copyDocumentRequest: CopyDocumentRequest)
      extends DocStoreLogic[CopyDocument200Response | CopyDocument404Response]
  case DeleteDocument(path: String)
      extends DocStoreLogic[DeleteDocument200Response | GetDocument404Response]
  case ListChildren(path: String)                  extends DocStoreLogic[List[String]]
  case Query(path: String, filter: Option[String]) extends DocStoreLogic[List[Json]]
  case GetDocument(path: String, version: Option[String])
      extends DocStoreLogic[Json | GetDocument404Response]
  case GetMetadata(path: String)              extends DocStoreLogic[GetMetadata200Response]
  case SaveDocument(path: String, body: Json) extends DocStoreLogic[SaveDocument200Response]
  case UpdateDocument(path: String, body: Json)
      extends DocStoreLogic[UpdateDocument200Response | GetDocument404Response]

object DocStoreLogic {
  import DocStoreLogic.*

  def query(path: String, filter: Option[String]): Program[DocStoreLogic, List[Value]] =
    Query(path, filter).asProgram

  def diffPaths(
      compareDocumentsRequest: CompareDocumentsRequest
  ): Program[DocStoreLogic, CompareDocuments200Response | CompareDocuments400Response] = {
    CompareDocuments(compareDocumentsRequest).asProgram
  }

  def copyPaths(
      copyDocumentRequest: CopyDocumentRequest
  ): Program[DocStoreLogic, CopyDocument200Response | CopyDocument404Response] = {
    CopyDocument(copyDocumentRequest).asProgram
  }

  def delete(
      path: String
  ): Program[DocStoreLogic, DeleteDocument200Response | GetDocument404Response] = {
    DeleteDocument(path).asProgram
  }
  def listChildren(
      path: String
  ): Program[DocStoreLogic, List[String]] = ListChildren(path).asProgram

  def get(
      path: String,
      version: Option[String]
  ): Program[DocStoreLogic, ujson.Value | GetDocument404Response] = {
    GetDocument(path, version).asProgram
  }

  def metadata(path: String): Program[DocStoreLogic, GetMetadata200Response] = GetMetadata(
    path
  ).asProgram

  def save(path: String, body: Json): Program[DocStoreLogic, SaveDocument200Response] = {
    SaveDocument(path, body).asProgram
  }

  def update(
      path: String,
      body: Json
  ): Program[DocStoreLogic, UpdateDocument200Response | GetDocument404Response] = {
    UpdateDocument(path, body).asProgram
  }
}
