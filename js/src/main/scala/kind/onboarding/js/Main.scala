package kind.onboarding

import kind.logic._
import kind.logic.js._
import kind.logic.js.goldenlayout._
import kind.logic.js.mermaid._
import kind.logic.js.scenarios._
import kind.logic.js.svg._
import kind.logic.js.tables._
import kind.logic.telemetry._
import kind.onboarding.PizzaHandler.InMemory
import kind.onboarding.PizzaLogic.PizzaOperation._
import org.scalajs.dom
import org.scalajs.dom.HTMLElement
import scalatags.JsDom.all._
import upickle.default._

import scala.scalajs.js.Dynamic.global
import scala.util.control.NonFatal

import PizzaLogic._

object MainPage {

  lazy val orderContainer  = initOrderDb()
  lazy val svgContainer    = initSvg()
  lazy val mermaidPage     = initMermaid()
  lazy val scenarioBuilder = ScenarioBuilder()

  // this just makes it easier for each component to "do stuff" (tm) w/ the test scenarios
  enum Outcome:
    case Success(
        scenario: TestScenario,
        request: MakePizzaRequest,
        handler: PizzaHandler.InMemory,
        logic: PizzaApp.App,
        result: Pizza
    )
    case Failure(scenario: TestScenario, error: Throwable)

  /** Most components will need to react to a new test scenario by running the app logic and then
    * displaying some result.mermaidPage This function is a convenience method for that behaviour
    */
  def withRequest(f: PartialFunction[Outcome, Unit]) = {
    EventBus.activeTestScenario.subscribe { scenario =>
      val outcome: Outcome =
        try {
          val request = read[MakePizzaRequest](scenario.input)

          val (impl, appLogic) = PizzaApp.inMemory(using Telemetry()).execOrThrow()

          // run the scenario
          val result = appLogic.orderPizza(request.quantity, request.toppings).execOrThrow()

          Outcome.Success(scenario, request, impl, appLogic, result)
        } catch {
          case NonFatal(e) =>
            println(s"Error on scenario $scenario: $e")
            Outcome.Failure(scenario, e)
        }
      if f.isDefinedAt(outcome) then {
        try {
          f(outcome)
        } catch {
          case NonFatal(e) =>
            println(s"component threw an error on scenario $scenario: $e")
        }
      }
    }
  }

  private def initOrderDb() = {
    val orderDbContainer = div().render
    withRequest { case Outcome.Success(scenario, request, handler, logic, result) =>
      orderDbContainer.innerHTML = ""

      val comp = TableComponent(
        DataSource.forRef(handler.ord.byIdRef, Seq("id", "quantity", "toppings")) {
          (map: Map[Long, SaveOrder], view) =>
            val data = map.toSeq

            val sorted = view.sortCol match {
              case Some(sort) =>
                val sorted = sort.column match {
                  case "id"       => data.sortBy(_._1)
                  case "quantity" => data.sortBy(_._2.quantity)
                  case "toppings" => data.sortBy(_._2.toppings.mkString(", "))
                }
                if sort.descending then sorted.reverse else sorted
              case _ => data
            }

            sorted
              .slice(view.fromRow, view.toRow)
              .map { (id, order) =>
                Row.forContent(
                  Seq(
                    id.toString(),
                    order.quantity.toString(),
                    order.toppings.mkString(", ")
                  )
                )
              }
        }
      )

      orderDbContainer.innerHTML = ""
      orderDbContainer.appendChild(comp.container)
    }
    orderDbContainer
  }

  private def initSvg() = {
    val appSvgContainer = div().render
    withRequest { case Outcome.Success(_, _, handler, _, _) =>
      val calls = handler.telemetry.calls.execOrThrow()
      appSvgContainer.innerHTML = ""
      appSvgContainer.appendChild(SvgForCalls(calls))
    }

    appSvgContainer
  }

  private def initMermaid() = {
    val appMermaidPage = MermaidPage()
    withRequest {
      case Outcome.Success(scenario, _, handler, _, _) =>
        val mermaidMarkdown = handler.telemetry.asMermaid().execOrThrow()
        appMermaidPage.update(scenario, mermaidMarkdown)
      case Outcome.Failure(scenario, e) =>
        appMermaidPage.updateError(
          scenario,
          s"We couldn't parse the scenario as a MakePizzaRequest: $e"
        )
    }

    appMermaidPage
  }
}

/** Our logic takes a few params as input, so here we wrap them up in someting we can marshal
  * to/from json
  */
case class MakePizzaRequest(quantity: Int, toppings: List[String]) derives ReadWriter {
  def toJson = writeJs(this)
}

@scala.scalajs.js.annotation.JSExportTopLevel("initLayout")
def initLayout(navId : String, myLayout: GoldenLayout) = {


  // global.window.initLayout = initLayout
  global.window.createNewComponent = createNewComponent
  global.window.onComponentCreated = onComponentCreated
  global.window.onComponentDestroyed = onComponentDestroyed

  // ensure we always have this scenario
  LocalState.addDefaultScenario(
    TestScenario(
      "Pizza Happy Path",
      "",
      MakePizzaRequest(1, List("cheese", "tomato sauce", "pepperoni")).toJson
    )
  )

  val drawer = HtmlUtils.$[HTMLElement](navId)

  myLayout.addMenuItem(drawer, "Scenario Builder") { state =>
    MainPage.scenarioBuilder.content
  }

  myLayout.addMenuItem(drawer, "Diagram") { state =>
    MainPage.mermaidPage.element
  }

  myLayout.addMenuItem(drawer, "SVG") { state =>
    MainPage.svgContainer
  }

  myLayout.addMenuItem(drawer, "Orders") { state =>
    MainPage.orderContainer
  }

  // Listen to tab change events to remove menu items
  EventBus.activeTabs.subscribe { activeTabs =>
    UIComponent.inactiveComoponents().foreach(_.showMenuItem())
    activeTabs.foreach(_.hideMenuItem())
  }

  myLayout.init()
}

