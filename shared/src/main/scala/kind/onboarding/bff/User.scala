package kind.onboarding.bff

import upickle.default.*
case class User(name: String, description: String, password: String, avatar: String)
    derives ReadWriter
