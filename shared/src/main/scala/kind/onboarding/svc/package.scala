package kind.onboarding

package object svc {

  type DocId = String

  def asId(name: String) = name.filter(_.isLetterOrDigit).toLowerCase
}
