package kind.onboarding.refdata

class Delegate(reader: CategoryService, writer: CategoryAdminService)
    extends CategoryService,
      CategoryAdminService {
  override def categories() = reader.categories()

  override def add(product: Category) = writer.add(product)

  override def update(product: Category) = writer.update(product)

  override def remove(product: String) = writer.remove(product)

  override def set(products: Seq[Category]) = writer.set(products)
}
