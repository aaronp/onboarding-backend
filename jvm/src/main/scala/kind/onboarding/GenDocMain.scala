package kind.onboarding

import kind.logic.*
import kind.logic.jvm.*
import kind.logic.telemetry.*

// this generates documentation in the 'docs' folder
@main def genDocs() = {
  val scenarios = List(Scenario("Happy Path", 2 -> List("cheese", "pepperoni"), asMermaid, "pizza"))

  GenDocs(scenarios)
}
