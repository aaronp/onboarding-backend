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

package kind.onboarding.docstore

def box(str: String): String = {
  val lines  = str.linesIterator.toList
  val maxLen = (0 +: lines.map(_.length)).max
  val boxed = lines.map { line =>
    s" | ${line.padTo(maxLen, ' ')} |"
  }
  val bar = " +-" + ("-" * maxLen) + "-+"
  (bar +: boxed :+ bar).mkString("\n")
}
