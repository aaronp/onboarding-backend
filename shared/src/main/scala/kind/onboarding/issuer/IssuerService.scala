package kind.onboarding.issuer

import kind.onboarding.docstore.DocStoreApp
import zio.Task

trait IssuerService {

  def listIssuers(nameFilter: String): Task[Seq[Issuer]]

  def addIssuer(issuer: Issuer): Task[Unit]

}

object IssuerService {
  def apply(docStore: DocStoreApp) = new IssuerService {

    override def listIssuers(nameFilter: String): Task[Seq[Issuer]] = {
//      docStore.getMetadata(
      ???
    }

    override def addIssuer(issuer: Issuer): Task[Unit] = ???
  }
}
