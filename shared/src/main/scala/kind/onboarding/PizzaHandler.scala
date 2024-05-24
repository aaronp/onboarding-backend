package kind.onboarding

import kind.logic._
import kind.logic.db._
import kind.logic.telemetry._
import zio._

import PizzaLogic._
import PizzaOperation._

/** The 'Handler' provides in implementation for our logic.
  *
  * You don't *need* to use this trait -- you could just implement the logic directly in your
  * PizzaApp.
  *
  * @param orderDb
  * @param financeDB
  */
trait PizzaHandler(val orderDb: DB[Long, SaveOrder], val financeDB: DB[Int, RecordOrder]) {

  def impl(using telemetry: Telemetry): [A] => PizzaOperation[A] => Result[A] = {
    [A] =>
      (_: PizzaOperation[A]) match {
        case command @ SaveOrder(_, _) =>
          orderDb
            .save(command)
            .map(_.toInt)
            .taskAsResultTraced(Actor.database("app", "DB"), command)
        case command @ Bake(toppings) =>
          Pizza(toppings).asResultTraced(Actor.service("app", "Kitchen"), command)
        case command @ Deliver(pizza) =>
          val price = 10 + (pizza.toppings.size * 1.75)
          price.asResultTraced(Actor.service("app", "Delivery"), command)
        case command @ RecordOrder(id, money) =>
          financeDB
            .save(command)
            .as(())
            .taskAsResultTraced(Actor.service("app", "Accounts"), command)

    }
  }
}

object PizzaHandler {

  type OrderDB = DB.InMemoryKeyValue[Long, PizzaOperation.SaveOrder]

  type FinanceDB = DB.InMemorySeq[PizzaOperation.RecordOrder]

  case class InMemory(val telemetry: Telemetry, ord: OrderDB, fin: FinanceDB)
      extends PizzaHandler(ord, fin) {
    def implementation = impl(using telemetry)
  }

  def inMemory(using telemetry: Telemetry): UIO[PizzaHandler.InMemory] = {
    for {
      orderDB   <- DB.inMemoryKeyValue[PizzaOperation.SaveOrder]
      financeDB <- DB.inMemory[PizzaOperation.RecordOrder]
    } yield new InMemory(telemetry, orderDB, financeDB)
  }

}
