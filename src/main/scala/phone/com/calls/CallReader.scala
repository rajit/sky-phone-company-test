package phone.com.calls

import cats.syntax.either._
import org.joda.time.format.PeriodFormatterBuilder
import phone.com.customer.CallLog
import squants.Time
import squants.time.TimeConversions._

object CallReader {
  case class FieldParseException(msg: String) extends RuntimeException(msg)
  private val durationFormatter =
    new PeriodFormatterBuilder()
      .appendHours
      .appendSuffix(":")
      .appendMinutes
      .appendSuffix(":")
      .appendSeconds
      .toFormatter

  def readDuration(duration: String): Either[Throwable, Time] =
    Either.catchNonFatal(durationFormatter.parsePeriod(duration))
      .map(_.toStandardDuration.getStandardSeconds.seconds)

  def readFields(row: String): Either[Throwable, (String, String, String)] =
    row.split(' ') match {
      case Array(id, number, duration) => Right((id, number, duration))
      case _ => Left(FieldParseException(s"Couldn't parse row into fields: $row"))
    }

  def read(record: String): Either[Throwable, CallLog.Call] =
    for {
      fields <- readFields(record)
      (rawId, rawNumber, rawDuration) = fields
      id <- CallLog.CustomerId.read(rawId).asRight[Throwable]
      number <- CallLog.PhoneNumber.read(rawNumber).asRight[Throwable]
      duration <- CallReader.readDuration(rawDuration)
    } yield CallLog.Call(id, number, duration)
}