package kind.onboarding.refdata

import zio.*
import kind.onboarding.docstore.*
import kind.onboarding.Systems.*
import kind.logic.telemetry.*
import kind.logic.*
import upickle.default.*

/** The write-end for creating categories, aimed at admins
  */
trait CategoryAdminService {
  def add(category: Category): Task[Unit]
  def update(category: Category): Task[Unit]
  def remove(category: String): Task[Unit]
  def set(categories: Seq[Category]): Task[Unit]
}

object CategoryAdminService {

  case class Impl(docStore: DocStoreApp)(using telemetry: Telemetry) extends CategoryAdminService {

    override def add(category: Category) = {
      docStore
        .saveDocument(s"$PathToCategories/${category.id}", category.asUJson)
        .asTaskTraced(
          CategoryAdmin.id,
          DB.id,
          category.withKey("category").merge("add".withKey("action"))
        )
        .as(())
    }

    override def update(category: Category) = {
      docStore
        .saveDocument(s"$PathToCategories/${category.id}", category.asUJson)
        .asTaskTraced(
          CategoryAdmin.id,
          DB.id,
          category.withKey("category").merge("update".withKey("action"))
        )
        .as(())
    }

    override def remove(category: String) = {
      docStore
        .deleteDocument(s"$PathToCategories/${asId(category)}")
        .asTaskTraced(
          CategoryAdmin.id,
          DB.id,
          category.withKey("category").merge("remove".withKey("action"))
        )
        .as(())
    }

    override def set(categories: Seq[Category]) = {
      for {
        _ <- docStore
          .deleteDocument(PathToCategories)
          .asTaskTraced(
            CategoryAdmin.id,
            DB.id,
            categories.withKey("category").merge("delete".withKey("action"))
          )
        _ <- ZIO.foreachPar(categories)(add)
      } yield ()
    }

  }

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

  def apply(db: Ref[Seq[Category]])                            = InMemory(db)
  def apply(docStore: DocStoreApp)(using telemetry: Telemetry) = Impl(docStore)
}
