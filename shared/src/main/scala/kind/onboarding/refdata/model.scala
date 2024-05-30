package kind.onboarding.refdata

import upickle.default.*

final case class Category(name: String, subCategories: Set[String] = Set.empty) derives ReadWriter
