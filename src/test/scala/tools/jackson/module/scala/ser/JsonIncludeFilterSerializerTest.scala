package tools.jackson.module.scala.ser

import com.fasterxml.jackson.annotation.JsonInclude
import tools.jackson.module.scala.{DefaultScalaModule, JacksonModule}

import scala.collection.{immutable, mutable}

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

  class FooMutableMapBean {
    @JsonInclude(content = JsonInclude.Include.CUSTOM, contentFilter = classOf[FooFilter])
    val stuff = new mutable.LinkedHashMap[String, String]()

    def add(key: String, value: String): FooMutableMapBean = {
      stuff.put(key, value)
      this
    }
  }

  class FooImmutableMapBean {
    @JsonInclude(content = JsonInclude.Include.CUSTOM, contentFilter = classOf[FooFilter])
    var stuff = new immutable.HashMap[String, String]()

    def add(key: String, value: String): FooImmutableMapBean = {
      stuff = stuff.updated(key, value)
      this
    }
  }
}

class JsonIncludeFilterSerializerTest extends SerializerTest {

  import JsonIncludeFilterSerializerTest._

  lazy val module: JacksonModule = DefaultScalaModule

  "An ObjectMapper with DefaultScalaModule" should "serialize mutable Map with a filter" in {
    val input = new FooMutableMapBean().add("a", "1").add("b", "foo").add("c", "2")
    newMapper.writeValueAsString(input) shouldEqual """{"stuff":{"a":"1","c":"2"}}"""
  }
  it should "serialize immutable Map with a filter" in {
    val input = new FooImmutableMapBean().add("a", "1").add("b", "foo").add("c", "2")
    newMapper.writeValueAsString(input) shouldEqual """{"stuff":{"a":"1","c":"2"}}"""
  }
}
