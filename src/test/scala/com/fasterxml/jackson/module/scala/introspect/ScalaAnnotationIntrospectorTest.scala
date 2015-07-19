package com.fasterxml.jackson
package module.scala
package introspect

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.ContextualSerializer
import com.fasterxml.jackson.databind.{SerializerProvider, JsonSerializer, BeanDescription, ObjectMapper}
import experimental.ScalaObjectMapper

import org.junit.runner.RunWith
import org.scalatest.{fixture, Matchers}
import org.scalatest.junit.JUnitRunner
import org.scalatest.LoneElement._

import beans.BeanProperty
import collection.JavaConverters._

object ScalaAnnotationIntrospectorTest
{
  class Token
  class BasicPropertyClass(val param: Int)
  class BeanPropertyClass(@BeanProperty val param: Int)
  class AnnotatedBasicPropertyClass(@JsonScalaTestAnnotation val param: Token)
  class AnnotatedBeanPropertyClass(@JsonScalaTestAnnotation @BeanProperty val param: Token)
}

@RunWith(classOf[JUnitRunner])
class ScalaAnnotationIntrospectorTest extends fixture.FlatSpec with Matchers {
  import ScalaAnnotationIntrospectorTest._

  type FixtureParam = ObjectMapper with ScalaObjectMapper

  override def withFixture(test: OneArgTest) = {
    val mapper = new ObjectMapper with ScalaObjectMapper
    mapper.registerModule(DefaultScalaModule)
    withFixture(test.toNoArgTest(mapper))
  }

  behavior of "ScalaAnnotationIntrospector"

  it should "detect a val property" in { mapper =>
    val bean = new BasicPropertyClass(1)
    val allProps = getProps(mapper, bean)
    allProps.loneElement should have ('name ("param"))

    val prop = allProps.asScala.head
    prop should have (
      'hasField (true),
      'hasGetter (true),
      'hasConstructorParameter (true)
    )

    val accessor = prop.getAccessor
    accessor shouldNot be (null)
  }

  it should "detect annotations on a val property" in { mapper =>
    mapper.registerModule(new SimpleModule() {
      addSerializer(new JsonSerializer[Token] with ContextualSerializer {
        override val handledType = classOf[Token]
        override def serialize(value: Token, gen: JsonGenerator, serializers: SerializerProvider): Unit =
          gen.writeString("")
        override def createContextual(prov: SerializerProvider, property: databind.BeanProperty): JsonSerializer[_] = {
          property.getAnnotation(classOf[JsonScalaTestAnnotation]) shouldNot be (null)
          this
        }
      })
    })

    val bean = new AnnotatedBasicPropertyClass(new Token)
    mapper.writeValueAsString(bean)
  }

  it should "detect a bean property" in { mapper =>
    val bean = new BeanPropertyClass(1)
    val allProps = getProps(mapper, bean)
    allProps.loneElement should have ('name ("param"))

    val prop = allProps.asScala.head
    prop should have (
      'hasField (true),
      'hasGetter (true),
      'hasConstructorParameter (true)
    )

    val accessor = prop.getAccessor
    accessor shouldNot be (null)
  }

  it should "detect annotations on a bean property" in { mapper =>
    mapper.registerModule(new SimpleModule() {
      addSerializer(new JsonSerializer[Token] with ContextualSerializer {
        override val handledType = classOf[Token]
        override def serialize(value: Token, gen: JsonGenerator, serializers: SerializerProvider): Unit =
          gen.writeString("")
        override def createContextual(prov: SerializerProvider, property: databind.BeanProperty): JsonSerializer[_] = {
          property.getAnnotation(classOf[JsonScalaTestAnnotation]) shouldNot be (null)
          this
        }
      })
    })

    val bean = new AnnotatedBeanPropertyClass(new Token)
    val allProps = getProps(mapper, bean)
    allProps.loneElement should have ('name ("param"))

    val prop = allProps.asScala.head
    prop should have (
      'hasField (true),
      'hasGetter (true),
      'hasConstructorParameter (true)
    )
    val param = prop.getConstructorParameter
    param.getAnnotation(classOf[JsonScalaTestAnnotation]) shouldNot be (null)

    mapper.writeValueAsString(bean)
  }

  private def getProps(mapper: ObjectMapper, bean: AnyRef) = {
    val config = mapper.getSerializationConfig
    val beanDescription: BeanDescription = config.introspect(mapper.constructType(bean.getClass))
    beanDescription.findProperties()
  }
}
