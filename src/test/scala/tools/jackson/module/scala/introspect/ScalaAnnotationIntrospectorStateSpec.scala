package tools.jackson.module.scala.introspect

import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.scala.ScalaModule
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

case class IntrospectorStateHolder(valueLong: Option[Long])

/**
 * The introspector's caches and its registered referenced value types belong to a module instance,
 * so a mapper built from its own ScalaModule does not share them with every other mapper.
 */
class ScalaAnnotationIntrospectorStateSpec extends AnyWordSpec with Matchers {

  "ScalaAnnotationIntrospectorModule" should {
    "give each builder its own introspector instance" in {
      val first = ScalaModule.builder()
      val second = ScalaModule.builder()
      first.scalaAnnotationIntrospectorModule should not be
        theSameInstanceAs(second.scalaAnnotationIntrospectorModule)
      first.scalaAnnotationIntrospectorModule should not be
        theSameInstanceAs(ScalaAnnotationIntrospectorModule)
    }
    "keep a referenced value type registration to the builder it was made on" in {
      val builder = ScalaModule.builder().addAllBuiltinModules()
      try {
        builder.scalaAnnotationIntrospectorModule
          .registerReferencedValueType(classOf[IntrospectorStateHolder], "valueLong", classOf[Long])
        builder.scalaAnnotationIntrospectorModule
          .getRegisteredReferencedValueType(classOf[IntrospectorStateHolder], "valueLong") shouldEqual Some(classOf[Long])
        // the module object knows nothing of it
        ScalaAnnotationIntrospectorModule
          .getRegisteredReferencedValueType(classOf[IntrospectorStateHolder], "valueLong") shouldBe empty
      } finally {
        builder.scalaAnnotationIntrospectorModule.clearRegisteredReferencedTypes()
      }
    }
    "still recognise the module object when removing it from a builder" in {
      val builder = ScalaModule.builder().addAllBuiltinModules()
      builder.hasModule(ScalaAnnotationIntrospectorModule) shouldEqual true
      builder.removeModule(ScalaAnnotationIntrospectorModule)
      builder.hasModule(ScalaAnnotationIntrospectorModule) shouldEqual false
    }
    "read a case class through a module built by the builder" in {
      val mapper = JsonMapper.builder().addModule(ScalaModule.builder().addAllBuiltinModules().build()).build()
      val value = IntrospectorStateHolder(Some(3L))
      mapper.readValue(mapper.writeValueAsString(value), classOf[IntrospectorStateHolder]) shouldEqual value
    }
  }
}
