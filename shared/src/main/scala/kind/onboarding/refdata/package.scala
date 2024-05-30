package kind.onboarding

import upickle.default.*
import kind.logic.*

package object refdata {

  val PathToCategories = "refdata/categories"

  def asId(name: String) = s"id-${name.filter(_.isLetterOrDigit)}"
  def asCategory(json: Json) = {
    try {
      Option(read[Category](json))
    } catch {
      case e: Exception =>
        println(s"Error parasing ${json} as category: $e")
        None
    }
  }

}
