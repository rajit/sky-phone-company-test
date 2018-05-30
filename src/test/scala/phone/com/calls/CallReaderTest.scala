package phone.com.calls

import org.specs2.mutable
import squants.time.TimeConversions._

class CallReaderTest extends mutable.Specification {
  "CallReader.readDuration should" >> {
    "parse 00:00:00 as 0 seconds" >> {
      CallReader.readDuration("00:00:00") must beRight(0.seconds)
    }
    "parse 00:59:17 as 3557 seconds" >> {
      CallReader.readDuration("00:59:17") must beRight(3557.seconds)
    }
    "parse 37:24:59 as 134699 seconds" >> {
      CallReader.readDuration("37:24:59") must beRight(134699.seconds)
    }
    "parse :: as invalid" >> {
      CallReader.readDuration("::") must beLeft
    }
    "parse asdf as invalid" >> {
      CallReader.readDuration("asdf") must beLeft
    }
  }
}
