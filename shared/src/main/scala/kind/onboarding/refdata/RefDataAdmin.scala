package kind.onboarding.refdata

import zio.{Ref, Task}

/** The write-end for creating products, aimed at admins
  */
trait RefDataAdmin {
  def add(product: Product): Task[Unit]
  def update(product: Product): Task[Unit]
  def remove(product: String): Task[Unit]
  def set(products: Seq[Product]): Task[Unit]
}

object RefDataAdmin {

  case class InMemory(db: Ref[Seq[Product]]) extends RefDataAdmin {
    def add(product: Product)   = db.update(list => product +: list)
    def remove(product: String) = db.update(_.filterNot(_.name == product))
    def update(product: Product) = db.get.flatMap { list =>
      list.find(_.name == product.name) match {
        case Some(found) =>
          for {
            _ <- remove(product.name)
            _ <- add(product)
          } yield ()
        case None => add(product)
      }
    }
    def set(products: Seq[Product]) = db.set(products)
  }

  def inMemory: Task[InMemory] = Ref.make(Seq.empty[Product]).map(apply)

  def apply(db: Ref[Seq[Product]]) = InMemory(db)
}
