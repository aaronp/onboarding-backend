package kind.onboarding.docstore

import kind.onboarding.docstore.api._

// TODO - write your business logic for your services here (the defaults all return 'not implemented'):
val myDefaultService: DefaultService = ???

/** This is your main entry point for your REST service It extends BaseApp which defines the
  * business logic for your services
  */
object RestMain extends BaseApp(appDefaultService = myDefaultService):
  start()
