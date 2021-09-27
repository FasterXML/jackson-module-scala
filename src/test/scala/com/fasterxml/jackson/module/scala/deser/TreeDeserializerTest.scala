package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.module.scala.deser.TreeDeserializerTest.TestBean
import com.fasterxml.jackson.module.scala.{ClassTagExtensions, DefaultScalaModule}

object TreeDeserializerTest {
  //https://github.com/FasterXML/jackson-module-scala/issues/261
  case class TestBean[A](var value: A)
}

class TreeDeserializerTest extends DeserializerTest {
  lazy val module = DefaultScalaModule

  "An ObjectMapper" should "deserialize from a tree" in {
    val json = """{"value": "WEEKLY"}"""

    val mapper = newBuilder.build() :: ClassTagExtensions
    val directBean = deserialize(json, new TypeReference[TestBean[DataPeriod]]{})
    directBean.value shouldEqual DataPeriod.WEEKLY
    val directBean2 = mapper.readValue[TestBean[DataPeriod]](json)
    directBean2.value shouldEqual DataPeriod.WEEKLY
    val indirectBean = mapper.treeToValue[TestBean[DataPeriod]](mapper.readTree(json))
    indirectBean.value shouldEqual DataPeriod.WEEKLY
  }

}
