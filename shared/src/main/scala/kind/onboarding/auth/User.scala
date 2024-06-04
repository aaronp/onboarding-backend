package kind.onboarding.auth

import upickle.default._
case class User(
    name: String,
    isEngineering: Boolean,
    isOperations: Boolean,
    isAuthenticatedUser: Boolean,
    avatar: String
) derives ReadWriter
