package com.fasterxml.jackson
package module.scala
package introspect

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.json.JsonFactory
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.{BeanDescription, ValueSerializer, ObjectMapper, SerializerProvider}
import org.scalatest.LoneElement.convertToCollectionLoneElementWrapper
import org.scalatest.Outcome
import org.scalatest.flatspec.FixtureAnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.beans.BeanProperty
import scala.collection.JavaConverters._

object ScalaAnnotationIntrospectorTest {
  class Token
  class BasicPropertyClass(val param: Int)
  class BeanPropertyClass(@BeanProperty val param: Int)
  class AnnotatedBasicPropertyClass(@JsonScalaTestAnnotation val param: Token)
  class AnnotatedBeanPropertyClass(@JsonScalaTestAnnotation @BeanProperty val param: Token)
  class JavaBeanPropertyClass {
    private var value: Int = 0
    @JsonProperty def getValue: Int = value
    @JsonProperty def setValue(value: Int): Unit = { this.value = value }
  }
}

class ScalaAnnotationIntrospectorTest extends FixtureAnyFlatSpec with Matchers {
  import ScalaAnnotationIntrospectorTest._

  type FixtureParam = ObjectMapper

  override def withFixture(test: OneArgTest): Outcome = {
    val builder = new JsonMapper.Builder(new JsonFactory).addModule(DefaultScalaModule)
    val mapper = builder.build()
    withFixture(test.toNoArgTest(mapper))
  }

  behavior of "ScalaAnnotationIntrospector"

  it should "detect a val property" in { mapper =>
    val bean = new BasicPropertyClass(1)
    val allProps = getProps(mapper, bean)
    allProps.loneElement should have (Symbol("name") ("param"))

    val prop = allProps.asScala.head
    prop should have (
      Symbol("hasField") (true),
      Symbol("hasGetter") (true),
      Symbol("hasConstructorParameter") (true)
    )

    val accessor = prop.getAccessor
    accessor shouldNot be (null)
  }

  it should "detect annotations on a val property" in { mapper =>
    val builder = new JsonMapper.Builder(new JsonFactory)
      .addModule(DefaultScalaModule)
      .addModule(new SimpleModule() {
      addSerializer(new ValueSerializer[Token] {
        override val handledType: Class[Token] = classOf[Token]
        override def serialize(value: Token, gen: JsonGenerator, serializers: SerializerProvider): Unit =
          gen.writeString("")
        override def createContextual(prov: SerializerProvider, property: databind.BeanProperty): ValueSerializer[_] = {
          property.getAnnotation(classOf[JsonScalaTestAnnotation]) shouldNot be (null)
          this
        }
      })
    })

    val bean = new AnnotatedBasicPropertyClass(new Token)
    builder.build().writeValueAsString(bean)
  }

  it should "detect a bean property" in { mapper =>
    val bean = new BeanPropertyClass(1)
    val allProps = getProps(mapper, bean)
    allProps.loneElement should have (Symbol("name") ("param"))

    val prop = allProps.asScala.head
    prop should have (
      Symbol("hasField") (true),
      Symbol("hasGetter") (true),
      Symbol("hasConstructorParameter") (true)
    )

    val accessor = prop.getAccessor
    accessor shouldNot be (null)
  }

  it should "detect annotations on a bean property" in { mapper =>
    val builder = new JsonMapper.Builder(new JsonFactory).addModule(new SimpleModule() {
      addSerializer(new ValueSerializer[Token] {
        override val handledType: Class[Token] = classOf[Token]
        override def serialize(value: Token, gen: JsonGenerator, serializers: SerializerProvider): Unit =
          gen.writeString("")
        override def createContextual(prov: SerializerProvider, property: databind.BeanProperty): ValueSerializer[_] = {
          property.getAnnotation(classOf[JsonScalaTestAnnotation]) shouldNot be (null)
          this
        }
      })
    })

    val bean = new AnnotatedBeanPropertyClass(new Token)
    val allProps = getProps(mapper, bean)
    allProps.loneElement should have (Symbol("name") ("param"))

    val prop = allProps.asScala.head
    prop should have (
      Symbol("hasField") (true),
      Symbol("hasGetter") (true),
      Symbol("hasConstructorParameter") (true)
    )
    val param = prop.getConstructorParameter
    param.getAnnotation(classOf[JsonScalaTestAnnotation]) shouldNot be (null)

    //TODO fix test (works in v2.12.0)
    //builder.build().writeValueAsString(bean)
  }

  it should "correctly infer name(s) of un-named bean properties" in { mapper =>
    val tree = mapper.valueToTree[databind.node.ObjectNode](new JavaBeanPropertyClass)
    tree.has("value") shouldBe true
    tree.has("setValue") shouldBe false
    tree.has("getValue") shouldBe false
  }

  private def getProps(mapper: ObjectMapper, bean: AnyRef) = {
    val classIntrospector = mapper.serializationConfig().classIntrospectorInstance()
    val beanDescription: BeanDescription = classIntrospector.introspectForSerialization(mapper.constructType(bean.getClass))
    beanDescription.findProperties()
  }
}
