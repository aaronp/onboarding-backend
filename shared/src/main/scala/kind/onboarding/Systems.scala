package kind.onboarding

import kind.logic._

/** List of the components in our system, used for telemetry/tracing
  *
  * @param id
  *   the actor ID for this component
  */
enum Systems(val id: Actor):
  case BFF extends Systems(Actor.service(Systems.Namespace, "bff"))
  case DB  extends Systems(Actor.database(Systems.Namespace, "DB"))
  // case Auth extends Systems(Actor.service(Systems.Namespace, "auth"))

  case OnboardingSvc extends Systems(Actor.service(Systems.RefData, "onboarding-service"))

  case CategoryRead  extends Systems(Actor.service(Systems.RefData, "categories"))
  case CategoryAdmin extends Systems(Actor.service(Systems.RefData, "categories-admin"))

object Systems {
  def Namespace = "onboarding"
  def RefData   = "refData"
}
