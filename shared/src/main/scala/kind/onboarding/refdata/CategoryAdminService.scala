package kind.onboarding.refdata

import zio.{Ref, Task}

/** The write-end for creating categories, aimed at admins
  */
trait CategoryAdminService {
  def add(category: Category): Task[Unit]
  def update(category: Category): Task[Unit]
  def remove(category: String): Task[Unit]
  def set(categories: Seq[Category]): Task[Unit]
}

object CategoryAdminService {

  case class InMemory(db: Ref[Seq[Category]]) extends CategoryAdminService {
    def add(category: Category)  = db.update(list => category +: list)
    def remove(category: String) = db.update(_.filterNot(_.name == category))
    def update(category: Category) = db.get.flatMap { list =>
      list.find(_.name == category.name) match {
        case Some(found) =>
          for {
            _ <- remove(category.name)
            _ <- add(category)
          } yield ()
        case None => add(category)
      }
    }
    def set(categories: Seq[Category]) = db.set(categories)
  }

  def inMemory: Task[InMemory] = Ref.make(Seq.empty[Category]).map(apply)

  def apply(db: Ref[Seq[Category]]) = InMemory(db)
}
