package nb.utils

import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormatter, ISODateTimeFormat}

import scala.util.Try

object DateTimeConversions extends DateTimeConversions
trait DateTimeConversions {

//  val simpleFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")
  val dtToEpoc: DateTime => Long = dt => dt.getMillis / 1000
  val epocToDt: Long => DateTime = l => new DateTime(l * 1000)

  implicit class str(s: String) {
    val parserISO: DateTimeFormatter = ISODateTimeFormat.dateTime()

    def tryParse: Try[Long] = Try {
      val dt = DateTime.parse(s, parserISO)
      dtToEpoc(dt)
    }.recoverWith {
      case _: IllegalArgumentException =>
        Try(s.toLong)
    }
  }
}