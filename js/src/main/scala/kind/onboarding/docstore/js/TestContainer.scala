package kind.onboarding.docstore.js

import kind.logic.js.*
import kind.logic.telemetry.*
import kind.onboarding.docstore.api.DefaultService
import kind.onboarding.docstore.model.*
import org.scalajs.dom
import org.scalajs.dom.html
import scalatags.JsDom.all.*

import scala.util.*
import scala.util.control.NonFatal
case class TestContainer(service: DefaultService, telemetry: Telemetry) {

  val ButtonStyle = "margin-left:1em;margin-right:1em;"
  val InputStyle  = "margin:1em;padding:1em;width:80%;font-size: 1.2em;"
  val SecondaryInputStyle =
    "margin-top:0.5em;padding-left:0.5em;padding-left:0.5em;width:80%;font-size: 1em;"

  private val messageDiv = div(
    style := "margin:0.5em;padding:0.5em;width:80%;font-size: 1em;"
  ).render

  val DefaultJson = """ { "a": 1, "b": 2 }  """
  private val jasonTextInput = textarea(
    rows  := 40,
    style := "margin:0.5em;padding:0.5em;width:80%;font-size: 1em;",
    value := DefaultJson
  )(DefaultJson).render
  private val pathDiv = div(
    style := "margin:0.5em;padding:0.5em;width:80%;font-size: 1em;"
  ).render
  private val pathInput =
    input(style := InputStyle, `type` := "text", value := "/a/b/c").render

  private val otherPathInput =
    input(style := SecondaryInputStyle, `type` := "text").render

  def onDiff(): Unit = {

    service.compareDocuments(CompareDocumentsRequest(Option(path), Option(otherPath))) match {
      case CompareDocuments200Response(Some(diffJson)) =>
        updateMessage("")
        updateResult(diffJson)
      case CompareDocuments200Response(None) =>
        updateMessage(s"No difference between ${path} and ${otherPath}")
      case err =>
        updateMessage(s"Error computing difference between ${path} and ${otherPath}: $err")
    }

  }

  def onCopy(): Unit = updateMessage {
    service.copyDocument(CopyDocumentRequest(Option(path), Option(otherPath))) match {
      case CopyDocument200Response(ok) => s"copied ${path} to ${otherPath}"
      case CopyDocument404Response(no) =>
        s"copy field for ${path} to ${otherPath}"
    }
  }

  pathInput.onkeyup = { (e: dom.KeyboardEvent) =>
    if (e.keyCode == 13) {
      onPathChanged()
    } else {
      onPathChanged()
    }
  }

  def path      = pathInput.value
  def otherPath = otherPathInput.value

  def updateResult(f: => Json): Unit = {
    try {
      jasonTextInput.value = f.render(2)
    } catch {
      case NonFatal(e) =>
        updateMessage("Error rendering json: " + e.getMessage)
    }
  }

  def updateMessage(f: => String): Unit = {
    val result: String =
      try {
        val content: String = f
        content
      } catch {
        case NonFatal(e) => s"Error ${path}: $e"
      }

    pathDiv.textContent = path
    messageDiv.textContent = result
  }

  def onPathChanged(): Unit = updateMessage {
    service
      .getDocument(path, None) match {
      case doc: Json if doc.isNull =>
        // leave the result ... let the user easily duplicate
        ""
      case doc: Json =>
        updateResult(doc)

        doc.render(2)
      case GetDocument404Response(msg) => s"Service returned 404: $msg"
    }
  }

  def currentJson: Try[Json] = {
    val c = jasonTextInput.value
    try {
      val jason = ujson.read(c)
      Success(jason)
    } catch {
      case NonFatal(e) => Failure(new Exception(s"Error parsing json >$c<: $e"))
    }
  }

  def onPatch() = updateMessage {
    currentJson match {
      case Success(json) =>
        val response = service.updateDocument(path, json)
        s"Update returned: $response"
      case Failure(err) => s"The value isn't valid json: $err"
    }
  }

  def onReload() = {
    service.getDocument(path, None) match {
      case value: Json                      => updateResult(value)
      case GetDocument404Response(notFound) => updateMessage(s"$path Not found: $notFound")
    }
  }
  def onSave() = updateMessage {
    currentJson match {
      case Success(json) =>
        val SaveDocument200Response(msg) = service.saveDocument(path, json)
        s"Save returned: $msg"
      case Failure(err) => s"The value isn't valid json: $err"
    }
  }

  val content = div(style := "")(
    div()(
      h1("Test Container"),
      span(style := "margin:1em;padding:1em", "Path:", pathInput),
      hr(),
      div(
        style := "margin:1em;padding:1em",
        "Diff/Copy With:",
        otherPathInput
      ),
      button(style := ButtonStyle, "Save", onclick   := { () => onSave() }),
      button(style := ButtonStyle, "Update", onclick := { () => onPatch() }),
      button(style := ButtonStyle, "Reload", onclick := { () => onReload() }),
      button(style := ButtonStyle, "Diff", onclick   := { () => onDiff() }),
      button(style := ButtonStyle, "Copy", onclick   := { () => onCopy() }),
      pathDiv,
      jasonTextInput,
      messageDiv
    )
  ).render

}
