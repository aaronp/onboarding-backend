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

// this model was generated using modelTest.mustache
package kind.onboarding.docstore.model

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import scala.util.*

class CopyDocumentRequestTest extends AnyWordSpec with Matchers {

  "CopyDocumentRequest.fromJson" should {
    """not parse invalid json""" in {
      val Failure(err) = Try(CopyDocumentRequestData.fromJsonString("invalid jason"))
      err.getMessage should startWith("Error parsing json 'invalid jason'")
    }
    """parse """ ignore {
      val Failure(err: ValidationErrors) =
        CopyDocumentRequestData.fromJsonString("""""").validated()

      sys.error("TODO")
    }
  }

}
