package tools.jackson.module.scala

import tools.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

case class NotDerived(aLong: Option[Long])

/** Scala 2 has nothing to derive from, so nothing is registered and the manual route is unaffected. */
class NoDerivedTypeInfoSpec extends AnyWordSpec with Matchers {
  "ScalaAnnotationIntrospectorModule on Scala 2" should {
    "register nothing of its own" in {
      ScalaAnnotationIntrospectorModule.clearRegisteredReferencedTypes()
      ScalaAnnotationIntrospectorModule
        .getRegisteredReferencedValueType(classOf[NotDerived], "aLong") shouldBe empty
    }
  }
}
