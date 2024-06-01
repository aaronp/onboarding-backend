package kind.onboarding.auth

import upickle.default.*
case class User(
    name: String,
    isEngineering: Boolean,
    isOperations: Boolean,
    isAuthenticatedUser: Boolean,
    avatar: String
) derives ReadWriter
