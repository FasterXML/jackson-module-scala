package tools.jackson.module.scala

import tools.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

case class NotDerived(aLong: Option[Long])

/**
 * A class that captured no ScalaTypeInfo - every class on Scala 2, and one without the `derives` on
 * Scala 3 - registers nothing, so the manual route is unaffected.
 */
class NoDerivedTypeInfoSpec extends AnyWordSpec with Matchers {
  "ScalaAnnotationIntrospectorModule" should {
    "register nothing for a class that derived nothing" in {
      ScalaAnnotationIntrospectorModule.clearRegisteredReferencedTypes()
      ScalaAnnotationIntrospectorModule
        .getRegisteredReferencedValueType(classOf[NotDerived], "aLong") shouldBe empty
    }
  }
}
