package kind.onboarding.svc

import upickle.default.ReadWriter

/** An onboarding doc needs to have at least a name, a category, and a subcategory
  *
  * @param name
  */
case class DraftDoc(name: String, category: String, subCategory: String) derives ReadWriter {
  def approve(flag: Boolean) = ApprovedDoc(name, category, subCategory, approved = flag)
}

case class ApprovedDoc(name: String, category: String, subCategory: String, approved: Boolean)
    derives ReadWriter
