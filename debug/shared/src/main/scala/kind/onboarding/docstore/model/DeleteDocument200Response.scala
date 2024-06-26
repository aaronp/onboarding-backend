/** Document Storage API API for storing, retrieving, updating, and deleting JSON documents.
  *
  * OpenAPI spec version: 1.1.0
  *
  * Contact: team@openapitools.org
  *
  * NOTE: This class is auto generated by OpenAPI Generator.
  *
  * https://openapi-generator.tech
  */

// this model was generated using model.mustache
package kind.onboarding.docstore.model
import scala.util.control.NonFatal

// see https://com-lihaoyi.github.io/upickle/
import upickle.default.{ReadWriter => RW, macroRW}
import upickle.default.*

case class DeleteDocument200Response(
    message: Option[String] = None
) {

  def asJson: String = asData.asJson

  def asData: DeleteDocument200ResponseData = {
    DeleteDocument200ResponseData(
      message = message.getOrElse("")
    )
  }

}

object DeleteDocument200Response {

  given RW[DeleteDocument200Response] =
    DeleteDocument200ResponseData.readWriter.bimap[DeleteDocument200Response](_.asData, _.asModel)

  enum Fields(fieldName: String) extends Field(fieldName) {
    case message extends Fields("message")
  }

}
