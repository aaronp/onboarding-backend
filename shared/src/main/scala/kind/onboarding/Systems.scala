package kind.onboarding

import kind.logic.*

/** List of the components in our system, used for telemetry/tracing
  *
  * @param id
  *   the actor ID for this component
  */
enum Systems(val id: Actor):
  case BFF  extends Systems(Actor.service(Systems.Namespace, "bff"))
  case DB   extends Systems(Actor.database(Systems.Namespace, "DB"))
  case Auth extends Systems(Actor.service(Systems.Namespace, "auth"))

  case CategoryRead  extends Systems(Actor.service(Systems.RefData, "categories"))
  case CategoryAdmin extends Systems(Actor.service(Systems.RefData, "categories-admin"))

object Systems {
  val Namespace = "onboarding"
  val RefData   = "refData"
}
