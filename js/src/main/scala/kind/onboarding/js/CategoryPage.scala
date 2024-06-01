package kind.onboarding.js

import scala.scalajs.js.annotation.*
import kind.onboarding.refdata.Category
import scala.scalajs.js.JSConverters._
import kind.logic._
import kind.onboarding.bff.LabeledValue

@JSExportAll
case class CategoryPage(services: Services) {

  private def loadCategories(): Seq[Category] = {
    try {
      services.bff.listCategories().execOrThrow()
    } catch {
      case e =>
        println(s"Error loading categories: $e")
        Nil
    }
  }

  private var _allCategories: Seq[Category] = loadCategories()

  private var _current: Option[Category] = None

  def options = _allCategories.map { case Category(name, _) =>
    LabeledValue(name).asJSON
  }.toJSArray

  private def current: Category = _current.getOrElse {
    val c = Category("default")
    _current = Some(c)
    c
  }

  private def current_=(newValue: Category) = {
    _current = Some(newValue)
  }

  def selectedCategoryName = current.name

  def subCategories = current.subCategories.toJSArray

  def onAddCategory(name: String) = {
    val newCategory = services.bff.addCategory(name).execOrThrow()
    current = newCategory

    // reload
    _allCategories = loadCategories()
  }

  def onAddSubCategory(name: String) = {
    current = current.copy(subCategories = current.subCategories + name)
    updateCategory()
  }

  def onRemoveSubCategory(name: String) = {
    current = current.copy(subCategories = current.subCategories - name)
    updateCategory()
  }

  private def updateCategory() = {
    val newCategory = services.bff.updateCategory(current).execOrThrow()
    current = newCategory
    _allCategories = loadCategories()
  }
  def selectedCategoryName_=(newValue: String): Unit = {
    _allCategories.find(_.name == newValue) match {
      case Some(found) => current = found
      case None        => current = Category("")
    }
  }
}