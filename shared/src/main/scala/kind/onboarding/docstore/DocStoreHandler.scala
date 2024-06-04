package kind.onboarding.docstore

import kind.logic._
import kind.logic.json.Filter
import kind.logic.json._
import kind.logic.telemetry._
import kind.onboarding.Systems._
import kind.onboarding.docstore.model._
import zio._

object DocStoreHandler {
  def apply(db: PathTree = PathTree.forPath("")): InMemory = {
    val ref = Ref.make(db).execOrThrow()
    InMemory(ref)
  }

  case class InMemory(ref: Ref[PathTree]) extends DocStoreHandler(ref) {
    def asTree = ref.get
  }
}

/** The implementation of the DocStore commands
  */
trait DocStoreHandler(ref: Ref[PathTree]) {

  def onGetNode(command: DocStoreLogic.GetNode, path: String): Result[Option[PathTree]] = {
    val task = for {
      latest <- ref.get
      pathAsList = path.asPath.filterNot(_.isEmpty)
      children   = if pathAsList.isEmpty then Option(latest) else latest.at(pathAsList)
    } yield children

    task.taskAsResultTraced(DB.id, command)
  }

  def onListChildren(command: DocStoreLogic.ListChildren, path: String): Result[Seq[String]] = {
    def kids(latest: PathTree, pathList: Seq[String]) = {
      latest.at(pathList).fold(List.empty[String])(_.children.keySet.toList.sorted)
    }
    def rootKids(latest: PathTree) = latest.children.keySet.toList.sorted

    val task = for {
      latest <- ref.get
      pathAsList = path.asPath.filterNot(_.isEmpty)
      children   = if pathAsList.isEmpty then rootKids(latest) else kids(latest, pathAsList)
    } yield children

    task.taskAsResultTraced(DB.id, command)
  }

  def onQuery(command: DocStoreLogic.Query, path: String, filterOpt: Filter) = {
    val task = for {
      latest <- ref.get
      parts   = path.asPath
      results = latest.query(parts, filterOpt)
    } yield results
    task.taskAsResultTraced(DB.id, command)
  }
  def onCompare(
      command: DocStoreLogic.CompareDocuments,
      leftPath: String,
      rightPath: String
  ): Result[CompareDocuments200Response] = {
    val task = for {
      latest <- ref.get
      leftValue = latest.at(leftPath.asPath)
      leftData  = leftValue.map(_.data).getOrElse(ujson.Null)

      rightValue = latest.at(rightPath.asPath)
      rightData  = rightValue.map(_.data).getOrElse(ujson.Null)

      diff = leftData.diffWith(rightData)
    } yield CompareDocuments200Response(Option(diff))

    task.taskAsResultTraced(DB.id, command)
  }

  def onCopy(command: DocStoreLogic.CopyDocument, fromPath: String, toPath: String) = {
    val task = for {
      latest <- ref.get
      data = latest.at(fromPath.asPath) match {
        case Some(tree) => tree.data
        case None       => ujson.Null
      }
      _ <- ref
        .update(_.updateData(toPath.asPath, data))
      response = CopyDocument200Response()
    } yield response
    task.taskAsResultTraced(DB.id, command)
  }

  def onDelete(command: DocStoreLogic.DeleteDocument) = {
    val parts = command.path.asPath
    if (parts.isEmpty) {
      ZIO
        .succeed(
          DeleteDocument200Response(Option("invalid delete request with an empty path"))
        )
        .taskAsResultTraced(DB.id, command)
    } else {
      val task = for {
        msg <- ref.modify { latest =>
          latest.remove(parts) match {
            case Some(newTree) => s"removed ${command.path}" -> newTree
            case None          => "not found"                -> latest
          }
        }
      } yield DeleteDocument200Response(Option(msg))
      task.taskAsResultTraced(DB.id, command)
    }
  }

  def onGetMetadata(command: DocStoreLogic.GetMetadata, path: String) = {
    val task = for {
      latest <- ref.get
      value = latest.at(path.asPath)
      data  = value.map(_.data).getOrElse(ujson.Null)
    } yield GetMetadata200Response(latestVersion = Option(s"TODO: versions: ${data.render(2)}"))

    task.taskAsResultTraced(DB.id, command)
  }

  def onGetDocument(
      command: DocStoreLogic.GetDocument,
      path: String,
      versionOpt: Option[String]
  ) = {
    val fullPath = path.asPath.filter(_.nonEmpty)
    val task = for {
      latest <- ref.get
      value    = latest.at(fullPath ++ versionOpt.toList)
      data     = value.map(_.data).getOrElse(ujson.Null)
      response = if data.isNull then GetDocument404Response() else data
    } yield response

    task.taskAsResultTraced(DB.id, command)
  }

  def onPatchDocument(command: DocStoreLogic.UpdateDocument, path: String, newValue: Json) = {
    val task = for {
      _ <- ref
        .update(_.patchData(path.asPath, newValue))
      latest <- ref.get
      response = UpdateDocument200Response(Option(s"Updated: ${latest.formatted}"))
    } yield response
    task.taskAsResultTraced(DB.id, command)
  }

  def onSaveDocument(command: DocStoreLogic.SaveDocument, path: String, data: Json) = {
    val task = for {
      _ <- ref
        .update(_.updateData(path.asPath, data))
      latest <- ref.get
      response = SaveDocument200Response(Option(s"Updated: ${latest.formatted}"))
    } yield response
    task.taskAsResultTraced(DB.id, command)
  }

  def defaultProgram(using telemetry: Telemetry): [A] => DocStoreLogic[A] => Result[A] = [A] =>
    (input: DocStoreLogic[A]) => {
      val result = input match {
        case command @ DocStoreLogic.Query(path, filter) =>
          onQuery(command, path, filter)
        case command @ DocStoreLogic.GetNode(path)      => onGetNode(command, path)
        case command @ DocStoreLogic.ListChildren(path) => onListChildren(command, path)
        case command @ DocStoreLogic.CompareDocuments(
              CompareDocumentsRequest(Some(leftPath), Some(rightPath))
            ) =>
          onCompare(command, leftPath, rightPath)
        case command @ DocStoreLogic.CompareDocuments(request) =>
          ZIO
            .attempt(sys.error(s"invalid diff request $request"))
            .taskAsResultTraced(DB.id, command)
        case command @ DocStoreLogic.CopyDocument(
              CopyDocumentRequest(Some(fromPath), Some(toPath))
            ) =>
          onCopy(command, fromPath, toPath)
        case command @ DocStoreLogic.CopyDocument(CopyDocumentRequest(from, to)) =>
          ZIO
            .attempt(sys.error(s"invalid copy request $from to $to"))
            .taskAsResultTraced(DB.id, command)
        case command @ DocStoreLogic.DeleteDocument(request) =>
          onDelete(command)
        case command @ DocStoreLogic.GetDocument(path, versionOpt) =>
          onGetDocument(command, path, versionOpt)
        case command @ DocStoreLogic.GetMetadata(path) =>
          onGetMetadata(command, path)
        case command @ DocStoreLogic.SaveDocument(path, data) =>
          onSaveDocument(command, path, data)
        case command @ DocStoreLogic.UpdateDocument(path, newValue) =>
          onPatchDocument(command, path, newValue)
      }

      try {
        result.asInstanceOf[Result[A]]
      } catch {
        case e: ClassCastException =>
          sys.error(s"BUG: $input producued $result which threw $e")
      }
  }
}
