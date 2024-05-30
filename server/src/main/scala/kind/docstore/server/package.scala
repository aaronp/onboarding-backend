package kind.docstore

import kind.logic.json.*

package object server {

  extension (path : String) {
    def asPathAndId: (String, String) = {
      val parts = path.asPath
      parts.init.mkString("/") -> parts.lastOption.getOrElse("")
    }
  }
}
