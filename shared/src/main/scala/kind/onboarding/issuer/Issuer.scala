package kind.onboarding.issuer

import kind.onboarding.refdata.Logo
import upickle.default.*

case class Issuer(name: String, lei: String) derives ReadWriter
