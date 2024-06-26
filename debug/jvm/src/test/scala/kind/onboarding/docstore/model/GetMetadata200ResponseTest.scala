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
import java.time.OffsetDateTime

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import scala.util.*

class GetMetadata200ResponseTest extends AnyWordSpec with Matchers {

  "GetMetadata200Response.fromJson" should {
    """not parse invalid json""" in {
      val Failure(err) = Try(GetMetadata200ResponseData.fromJsonString("invalid jason"))
      err.getMessage should startWith("Error parsing json 'invalid jason'")
    }
    """parse """ ignore {
      val Failure(err: ValidationErrors) =
        GetMetadata200ResponseData.fromJsonString("""""").validated()

      sys.error("TODO")
    }
  }

}
