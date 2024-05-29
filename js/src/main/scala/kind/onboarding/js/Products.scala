package kind.onboarding.js
import kind.onboarding.refdata.*
import zio.ZIO

case class Products(reader: RefDataService, writer: RefDataAdmin)

object Products {
  def inMemory(): ZIO[Any, Throwable, Products] = for {
    reader <- RefDataService.inMemory
    writer = RefDataAdmin(reader.db)
  } yield Products(reader, writer)
}
