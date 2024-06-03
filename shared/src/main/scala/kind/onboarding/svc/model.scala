package kind.onboarding.svc

import upickle.default.ReadWriter

/** An onboarding doc needs to have at least a name, a category, and a subcategory
  *
  * @param name
  */
case class DraftDoc(name: String, category: String, subCategory: String, ownerUserId: String)
    derives ReadWriter {
  def approve(flag: Boolean) =
    ApprovedDoc(name, category, subCategory, ownerUserId, approved = flag)
}

case class ApprovedDoc(
    name: String,
    category: String,
    subCategory: String,
    ownerUserId: String,
    approved: Boolean
) derives ReadWriter
