package kind.onboarding.refdata

import zio.{Ref, Task}

/** The write-end for creating products, aimed at admins
  */
trait RefDataAdmin {
  def add(product: Product): Task[Unit]
  def set(products: Seq[Product]): Task[Unit]
}

object RefDataAdmin {

  case class InMemory(db: Ref[Seq[Product]]) extends RefDataAdmin {
    def add(product: Product)       = db.update(list => product +: list)
    def set(products: Seq[Product]) = db.set(products)
  }

  def inMemory: Task[InMemory] = Ref.make(Seq.empty[Product]).map(apply)

  def apply(db: Ref[Seq[Product]]) = InMemory(db)
}
