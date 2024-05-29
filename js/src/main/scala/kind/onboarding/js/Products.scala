package kind.onboarding.js
import kind.onboarding.refdata.*
import zio.ZIO

case class Products(reader: RefDataService, writer: RefDataAdmin)
    extends RefDataService
    with RefDataAdmin {
  override def products() = reader.products()

  def add(product: Product) = writer.add(product)

  def update(product: Product) = writer.update(product)

  def remove(product: String) = writer.remove(product)

  def set(products: Seq[Product]) = writer.set(products)
}

object Products {
  def inMemory(): ZIO[Any, Throwable, Products] = for {
    reader <- RefDataService.inMemory
    writer = RefDataAdmin(reader.db)
  } yield Products(reader, writer)
}
