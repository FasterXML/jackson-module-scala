package tools.jackson.module.scala.ser

import com.fasterxml.jackson.annotation.{JsonAutoDetect, PropertyAccessor}
import tools.jackson.databind.introspect.VisibilityChecker
import tools.jackson.module.scala.DefaultScalaModule

import scala.collection.mutable

case object CaseObjectExample {
  val field1: String = "test"
  val field2: Int = 42
}

// objects holding mutable state: what is written is the state at the time of writing
case object MutableCaseObjectExample {
  var count: Int = 0
}

object MutablePlainObjectExample {
  var count: Int = 0
}

case object MutableCollectionObjectExample {
  val names: mutable.ListBuffer[String] = mutable.ListBuffer.empty
}

case object PrivateVarObjectExample {
  private var hidden: Int = 1
  def peek: Int = hidden
}

class CaseObjectSerializerTest extends SerializerTest {

  case object Foo {
    val field: String = "bar"
  }

  def module = DefaultScalaModule

  "An ObjectMapper with the DefaultScalaModule" should "serialize a case object as a bean" in {
    serialize(CaseObjectExample) should (
       equal ("""{"field1":"test","field2":42}""") or
         equal ("""{"field2":42,"field1":"test"}""")
    )
  }

  // https://github.com/FasterXML/jackson-module-scala/issues/596
  it should "serialize a case object when visibility settings set" ignore {
    val mapper = newBuilder
      .changeDefaultVisibility(_ => {
        VisibilityChecker.defaultInstance()
          .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
          .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
      })
      .build()
    mapper.writeValueAsString(CaseObjectExample) should (
      equal("""{"field1":"test","field2":42}""") or
        equal("""{"field2":42,"field1":"test"}""")
      )
  }

  it should "serialize an inner case object when visibility settings set" in {
    val mapper = newBuilder
      .changeDefaultVisibility(_ => {
        VisibilityChecker.defaultInstance()
          .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
          .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
      })
      .build()
    mapper.writeValueAsString(Foo) shouldEqual """{"field":"bar"}"""
  }

  it should "serialize the current value of a var in a case object" in {
    try {
      serialize(MutableCaseObjectExample) shouldEqual """{"count":0}"""
      MutableCaseObjectExample.count = 5
      serialize(MutableCaseObjectExample) shouldEqual """{"count":5}"""
    } finally {
      MutableCaseObjectExample.count = 0
    }
  }

  it should "serialize the current value of a var in a plain object" in {
    try {
      serialize(MutablePlainObjectExample) shouldEqual """{"count":0}"""
      MutablePlainObjectExample.count = 3
      serialize(MutablePlainObjectExample) shouldEqual """{"count":3}"""
    } finally {
      MutablePlainObjectExample.count = 0
    }
  }

  it should "serialize the current contents of a mutable collection in a case object" in {
    try {
      serialize(MutableCollectionObjectExample) shouldEqual """{"names":[]}"""
      MutableCollectionObjectExample.names += "a"
      serialize(MutableCollectionObjectExample) shouldEqual """{"names":["a"]}"""
    } finally {
      MutableCollectionObjectExample.names.clear()
    }
  }

  it should "not serialize a private var in a case object" in {
    serialize(PrivateVarObjectExample) shouldEqual "{}"
  }
}
