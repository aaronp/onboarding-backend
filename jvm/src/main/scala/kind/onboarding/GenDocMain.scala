package kind.onboarding

import kind.logic.*
import kind.logic.jvm.*
import kind.logic.telemetry.*

import PizzaLogic.*
import PizzaOperation.*

// this generates documentation in the 'docs' folder
@main def genDocs() = {
  val scenarios =List(Scenario("Happy Path", 2 -> List("cheese", "pepperoni"), asMermaid, "pizza"))

  GenDocs(scenarios)
}

def asMermaid(input : (Int, List[String])) = {
    given telemetry: Telemetry = Telemetry()
    val (quantity, toppings) = input
    val program = PizzaApp {
      [A] =>
        (command: PizzaOperation[A]) =>
          command match {
            case command @ SaveOrder(_, _) => 1.asResultTraced(Actor.database("app", "DB"), command)
            case command @ Bake(toppings) =>
              Pizza(toppings).asResultTraced(Actor.service("app", "Kitchen"), command)
            case command @ Deliver(pizza) =>
              (12.34).asResultTraced(Actor.service("app", "Delivery"), command)
            case command @ RecordOrder(id, money) =>
              ().asResultTraced(Actor.service("app", "Accounts"), command)
        }
    }

    // run the program
    program.orderPizza(quantity, toppings).execOrThrow()

    // get the call stack as a mermaid diagram
    telemetry.asMermaidDiagram().execOrThrow()
}

