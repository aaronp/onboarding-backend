package kind.onboarding.refdata

import zio.*

trait CategoryService {
  def categories(): Task[Seq[Category]]
  def getCategory(name: String): Task[Option[Category]] = categories().map { list =>
    list.find(_.name == name)
  }
}

object CategoryService {
  case class InMemory(db: Ref[Seq[Category]]) extends CategoryService {
    override def categories(): Task[Seq[Category]] = db.get
  }

  def inMemory: Task[InMemory] = Ref.make(Seq.empty[Category]).map(apply)

  def apply(db: Ref[Seq[Category]]) = InMemory(db)
}
