package kind.onboarding.js

import upickle.default.*
case class User(name: String, description: String, password: String, avatar: String)
    derives ReadWriter {
  def asJson = writeJs(this)
  // the avatar is a base64 encoded string
}
