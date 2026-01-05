package tools.jackson.module.scala.ser

import com.fasterxml.jackson.annotation.JsonInclude
import tools.jackson.module.scala.{DefaultScalaModule, JacksonModule}

import scala.collection.mutable

object JsonIncludeFilterSerializerTest {
  class FooFilter {
    override def equals(other: Any): Boolean = {
      if (other == null) { // do NOT filter out nulls
        return false
      }
      // in fact, only filter out exact String "foo"
      "foo".equals(other)
    }
  }

  class FooMapBean {
    @JsonInclude(content = JsonInclude.Include.CUSTOM, contentFilter = classOf[FooFilter])
    val stuff = new mutable.LinkedHashMap[String, String]

    def add(key: String, value: String): FooMapBean = {
      stuff.put(key, value)
      this
    }
  }
}

class JsonIncludeFilterSerializerTest extends SerializerTest {

  import JsonIncludeFilterSerializerTest._

  lazy val module: JacksonModule = DefaultScalaModule

  "An ObjectMapper with DefaultScalaModule" should "serialize Map with a filter" in {
    val input = new FooMapBean().add("a", "1").add("b", "foo").add("c", "2")
    newMapper.writeValueAsString(input) shouldEqual """{"stuff":{"a":"1","c":"2"}}"""
  }
}
