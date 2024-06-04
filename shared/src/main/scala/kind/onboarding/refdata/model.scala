package kind.onboarding.refdata

import upickle.default._

final case class Category(name: String, subCategories: Set[String] = Set.empty) derives ReadWriter {
  def id = asId(name)
}
