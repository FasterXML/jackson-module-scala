package tools.jackson.module.scala.deser

import tools.jackson.module.scala.DefaultScalaModule

// see EnumerationDeserializerTest for tests that also pass in Scala3
class EnumerationScala2DeserializerTest extends DeserializerTest {

  lazy val module: DefaultScalaModule.type = DefaultScalaModule

  //TODO fix test (works in v2.18.0)
  it should "deserialize a set of weekdays" ignore {
    val container = new EnumSetContainer
    val json = newMapper.writeValueAsString(container)
    val result = deserialize(json, classOf[EnumSetContainer])
    result.days shouldEqual container.days
  }
}
