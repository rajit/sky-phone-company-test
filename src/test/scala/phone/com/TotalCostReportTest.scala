package phone.com

import cats.effect.IO
import cats.implicits._
import fs2._
import org.specs2.mutable
import squants.time.TimeConversions._
import phone.com.customer.CallLog

class TotalCostReportTest extends mutable.Specification {
  private val customerId1 =
    CallLog.CustomerId.read("A")
  private val customerId2 =
    CallLog.CustomerId.read("B")
  private val phoneNumber1 =
    CallLog.PhoneNumber.read("123-123-123")
  private val phoneNumber2 =
    CallLog.PhoneNumber.read("999-999-999")
  private val phoneNumber3 =
    CallLog.PhoneNumber.read("713-555-532")

  "TotalCostReport.report should" >> {
    "return empty report if stream of calls is empty" >> {
      TotalCostReport.report[Pure](Stream.empty).toList must_=== List("")
    }

    "correctly calculate single customer's single call" >> {
      // Only number is the greatest cost, so cost is £0
      val oneMinuteCall = CallLog.Call(customerId1, phoneNumber2, 1.minutes)
      TotalCostReport.report[Pure](Stream.emit(oneMinuteCall)).toList must_===
        List("Customer: A, Total cost: £0.00\n")
    }

    "correctly calculate single customer's two calls, the cheaper under 3 minutes" >> {
      val oneMinuteCall = CallLog.Call(customerId1, phoneNumber1, 1.minutes)
      val fourMinuteCall = CallLog.Call(customerId1, phoneNumber2, 4.minutes)
      TotalCostReport.report[Pure](Stream.emits(Seq(oneMinuteCall, fourMinuteCall))).toList must_===
        List("Customer: A, Total cost: £0.03\n")
    }

    "correctly calculate single customer's two calls, the cheaper over 3 minutes" >> {
      val oneMinuteCall = CallLog.Call(customerId1, phoneNumber1, 4.minutes)
      val fourMinuteCall = CallLog.Call(customerId1, phoneNumber2, 7.minutes)
      TotalCostReport.report[Pure](Stream.emits(Seq(oneMinuteCall, fourMinuteCall))).toList must_===
        List("Customer: A, Total cost: £0.07\n")
    }

    // Speculative requirement, may not expect to have calls of 0 duration
    "charge nothing for a call of 0 seconds duration, and a more costly call to a different number" >> {
      val zeroMinuteCall = CallLog.Call(customerId1, phoneNumber1, 0.minutes)
      val fourMinuteCall = CallLog.Call(customerId1, phoneNumber3, 4.minutes)
      TotalCostReport.report[Pure](Stream.emits(Seq(zeroMinuteCall, fourMinuteCall))).toList must_===
        List("Customer: A, Total cost: £0.00\n")
    }

    "correctly calculate two customer's single calls each, which should be free" >> {
      val firstCustomerCall = CallLog.Call(customerId1, phoneNumber1, 0.minutes)
      val secondCustomerCall = CallLog.Call(customerId2, phoneNumber1, 0.minutes)
      TotalCostReport.report[Pure](Stream.emits(Seq(firstCustomerCall, secondCustomerCall))).toList must_===
        List("Customer: A, Total cost: £0.00\nCustomer: B, Total cost: £0.00\n")
    }
  }

  "TotalCostReport.combineExcMax should" >> {
    "exclude 10 from 1, 5, 10 and sum to 6" >> {
      TotalCostReport.combineExcMax(List(1, 5, 10)) must_== 6
    }
    "sum 0 to 0" >> {
      TotalCostReport.combineExcMax(List(0)) must_== 0
    }
    "exclude a single 1 from sum 0, 1, 1, 1 and sum to 2" >> {
      TotalCostReport.combineExcMax(List(0, 1, 1, 1)) must_== 2
    }
  }

  "TotalCostReport.readCallsFromLog should" >> {
    "ignore empty lines" >> {
      TotalCostReport.readCallsFromLog[IO](Stream.emits(Seq("", "", ""))).compile.toList.unsafeRunSync() must_===
        List()
    }
    "read a single row correctly" >> {
      TotalCostReport.readCallsFromLog[IO](Stream.emit("C 932-123-753 00:01:00")).compile.toList.unsafeRunSync() must_===
        List(CallLog.Call(CallLog.CustomerId.read("C"), CallLog.PhoneNumber.read("932-123-753"), 60.seconds))
    }
    "ignore invalid rows" >> {
      TotalCostReport.readCallsFromLog[IO](Stream.emits(Seq("C 932-123-753 00:01:00", "asdf asdf"))).compile.toList.unsafeRunSync() must_===
        List(CallLog.Call(CallLog.CustomerId.read("C"), CallLog.PhoneNumber.read("932-123-753"), 60.seconds))
    }
  }
}
