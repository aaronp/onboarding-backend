package kind.onboarding.refdata

import kind.logic._
import kind.logic.telemetry._
import kind.onboarding.Systems._
import kind.onboarding.docstore.DocStoreApp
import zio._

trait CategoryService {
  def categories(): Task[Seq[Category]]
  def getCategory(name: String): Task[Option[Category]] = categories().map { list =>
    list.find(_.name == name)
  }
}

object CategoryService {

  case class Impl(docStore: DocStoreApp)(using telemetry: Telemetry) extends CategoryService {

    override def categories(): Task[Seq[Category]] = {
      docStore
        .query(PathToCategories, None)
        .asTaskTraced(CategoryAdmin.id, DB.id, "categories".withKey("action").asUJson)
        .map { found =>
          found.flatMap(asCategory).toSeq
        }
    }
  }

  case class InMemory(db: Ref[Seq[Category]]) extends CategoryService {
    override def categories(): Task[Seq[Category]] = db.get
  }

  def inMemory: Task[InMemory] = Ref.make(Seq.empty[Category]).map(apply)

  def apply(db: Ref[Seq[Category]])                            = InMemory(db)
  def apply(docStore: DocStoreApp)(using telemetry: Telemetry) = Impl(docStore)
}
