package kind.onboarding

import kind.logic._

import java.time.ZoneId
import java.time.ZonedDateTime

package object svc {

  type DocId = String

  extension (timestamp: ZonedDateTime) {
    def utc         = timestamp.format(java.time.format.DateTimeFormatter.ISO_INSTANT)
    def epochMillis = timestamp.toInstant().toEpochMilli()
  }

  extension (data: Json) {
    def withId(id: String) = data.merge(id.withKey("id"))
    def withTimestamp(now: ZonedDateTime = timestamp()) = {
      data
        .merge(now.utc.withKey("lastUpdated")) //
        .merge(now.epochMillis.withKey("lastUpdatedEpochMillis"))

    }
  }

  def asId(name: String) = name.filter(_.isLetterOrDigit).toLowerCase

  def timestamp(): ZonedDateTime = ZonedDateTime.now(ZoneId.of("UTC"))
}
