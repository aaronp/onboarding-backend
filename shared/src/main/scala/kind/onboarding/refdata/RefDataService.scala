package kind.onboarding.refdata

import zio.*

trait RefDataService {

  def load(): Task[RefData]

}

object RefDataService {}
