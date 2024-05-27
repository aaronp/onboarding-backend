package kind.onboarding

import kind.onboarding.PizzaHandler.InMemory
import kind.onboarding.PizzaLogic.PizzaOperation._

import kind.logic._
import kind.logic.telemetry._

import scala.scalajs.js.annotation.*
import PizzaLogic._

@JSExportAll
case class Services(handler: PizzaHandler.InMemory, app: PizzaApp.App) {
  def orderPizza(quantity: Int, toppings: List[String]): Pizza = {
    app.orderPizza(quantity, toppings).execOrThrow()
  }

  def submitOnboardingForm(json: String): Boolean | String = {
    if json.contains("meh") then s"returning text as $json" else true
//    s"dynamic? submitOnboardingForm with $json  where json is ${json.getClass}"
  }

  def test(x: Int): true | ujson.Value = {
    "Dave"
  }
}

object Services {

  @JSExportTopLevel("createService")
  def createService(): Services = {
    val (impl, appLogic) = PizzaApp.inMemory(using Telemetry()).execOrThrow()
    Services(impl, appLogic)
  }

}
