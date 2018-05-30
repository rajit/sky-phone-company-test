package phone.com.customer

import cats.{Eq, Order, Show}
import cats.instances.string._
import squants.Time

object CallLog {
  // "Opaque" types
  object CustomerId {
    implicit val idShow: Show[CustomerId] =
      Show.show(_.underlying)
    implicit val idOrder: Order[CustomerId] =
      Order.by(_.underlying)
    def read(in: String): CustomerId = new CustomerId(in)
  }
  final class CustomerId private (val underlying: String) extends AnyVal

  object PhoneNumber {
    implicit val phoneEq: Eq[PhoneNumber] =
      Eq.by(_.underlying)
    def read(in: String): PhoneNumber = new PhoneNumber(in)
  }
  final class PhoneNumber private (val underlying: String) extends AnyVal

  case class Call(customerId: CustomerId, phoneNumberCalled: PhoneNumber, callDuration: Time)
}
