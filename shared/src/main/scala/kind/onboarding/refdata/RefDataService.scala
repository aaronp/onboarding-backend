package kind.onboarding.refdata

import zio.*

trait RefDataService {
  def products(): Task[Seq[Product]]
}

object RefDataService {
  val TestData = Seq(
    Product("Toy", Set("puzzle", "board-game", "cuddly toy", "doll")),
    Product("Tool", Set("plumbing", "carpentry", "masonry", "DIY/home"))
  )

  case class InMemory(db: Ref[Seq[Product]]) extends RefDataService {
    override def products(): Task[Seq[Product]] = db.get
  }

  def fixed(data: Seq[Product] = TestData) = new RefDataService {
    override def products(): Task[Seq[Product]] = ZIO.succeed(data)
  }

  def inMemory: Task[InMemory] = Ref.make(Seq.empty[Product]).map(apply)

  def apply(db: Ref[Seq[Product]]) = InMemory(db)
}
