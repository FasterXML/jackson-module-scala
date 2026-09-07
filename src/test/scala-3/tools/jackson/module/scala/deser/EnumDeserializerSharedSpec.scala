package tools.jackson.module.scala.deser

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

enum SharedProbeEnum {
  case Red, Green, Blue
}

class EnumDeserializerSharedSpec extends AnyWordSpec with Matchers {

  "canFindByOrdinal" should {
    "answer true for an enum that has a case at ordinal 0" in {
      EnumDeserializerShared.canFindByOrdinal(classOf[SharedProbeEnum]) shouldBe true
    }
    "answer false for a class with no fromOrdinal at all" in {
      EnumDeserializerShared.canFindByOrdinal(classOf[String]) shouldBe false
    }
    "answer false when fromOrdinal fails the ordinary way" in {
      EnumDeserializerShared.canFindByOrdinal(classOf[OrdinalProbes.Absent]) shouldBe false
    }
    // reflection wraps whatever the method threw, so both of these reach the catch as an
    // InvocationTargetException and are told apart only by their cause
    "let a fatal error through rather than answering false" in {
      an[OutOfMemoryError] should be thrownBy
        EnumDeserializerShared.canFindByOrdinal(classOf[OrdinalProbes.Fatal])
    }
    "let an interrupt through rather than answering false" in {
      an[InterruptedException] should be thrownBy
        EnumDeserializerShared.canFindByOrdinal(classOf[OrdinalProbes.Interrupting])
    }
  }

  "matchByName" should {
    "find the case that goes by the name" in {
      EnumDeserializerShared.matchByName(classOf[SharedProbeEnum], "Green") shouldEqual Some(SharedProbeEnum.Green)
    }
    "find nothing for a name no case goes by" in {
      EnumDeserializerShared.matchByName(classOf[SharedProbeEnum], "Purple") shouldBe empty
    }
    "find nothing for a class with no case table" in {
      EnumDeserializerShared.matchByName(classOf[String], "Green") shouldBe empty
    }
    "answer without walking the ordinals" in {
      // this one answers fromOrdinal for every ordinal there is, so the walk it replaced would not
      // have ended. The read runs on its own thread so a regression reports rather than hangs.
      @volatile var result: Option[_] = null
      val reader = new Thread(() => result = EnumDeserializerShared.matchByName(classOf[OrdinalProbes.Unbounded], "case1"))
      reader.setDaemon(true)
      reader.start()
      reader.join(30000)
      withClue("matching a name against an endless case table did not terminate: ") {
        reader.isAlive shouldBe false
      }
      result shouldEqual Some("case1")
    }
  }
}
