package phone.com

import cats.effect.Sync
import cats.implicits._
import cats.{Monoid, Order}
import fs2.Stream
import phone.com.calls.CallReader
import phone.com.customer.CallLog
import phone.com.customer.CallLog.CustomerId
import squants.market.MoneyConversions._
import squants.time.TimeConversions._
import squants.{Money, Price, Time}

object TotalCostReport {
  implicit val costMonoid: Monoid[Money] =
    new Monoid[Money] {
      override def empty: Money = 0.GBP
      override def combine(x: Money, y: Money): Money = x.plus(y)
    }
  implicit val moneyOrder: Order[Money] =
    Order.from(_.compare(_))

  def log[F[_]](msg: String)(implicit F: Sync[F]): F[Unit] =
    F.delay(Console.err.println(msg))

  def excludeEmpty[F[_]](s: Stream[F, String]): Stream[F, String] =
    s.filter(!_.trim.isEmpty)

  // TODO: Move to Validated which would allow errors to be accumulated and propagated up and reported according to client
  def readCalls[F[_]: Sync](s: Stream[F, String]): Stream[F, CallLog.Call] =
    s.flatMap(call =>
      CallReader.read(call)
        .fold[Stream[F, CallLog.Call]](
        err => Stream.eval_(log(err.getMessage)),
        Stream.emit(_)))

  def readCallsFromLog[F[_]: Sync](calls: Stream[F, String]): Stream[F, CallLog.Call] =
    calls.through(excludeEmpty)
      .through(readCalls[F])

  def report[F[_]](calls: Stream[F, CallLog.Call]): Stream[F, String] =
    // Each Map will be folded into each either by their key, with the cost summed up by Monoid
    calls.foldMap(call => Map(call.customerId -> Map(call.phoneNumberCalled -> cost(call))))
      .map {
        _.mapValues(_.values) // Discard the phone number
          .mapValues(combineExcMax(_))
          .toList
          .sortBy(_._1)
          .map(reportShow)
          .mkString("")
      }

  def combineExcMax[A: Order: Monoid](numberCosts: Iterable[A]): A =
    numberCosts.foldLeft((none[A], Monoid[A].empty))((p, next) => {
      val (maybeMax, combined) = p
      maybeMax match {
        case None => (next.some, combined)
        case Some(notMax) if next > notMax => (next.some, combined.combine(notMax))
        case max => (max, combined.combine(next))
      }
    })._2 // Return total

  def cost(call: CallLog.Call): Money = getPrice(call) * call.callDuration

  def getPrice(call: CallLog.Call): Price[Time] = {
    if (call.callDuration < 3.minutes) 0.0005.GBP / second
    else 0.0003.GBP / second
  }

  def reportShow(customerCost: (CustomerId, Money)): String =
    s"Customer: ${customerCost._1.show}, Total cost: ${customerCost._2.toFormattedString}\n"
}
