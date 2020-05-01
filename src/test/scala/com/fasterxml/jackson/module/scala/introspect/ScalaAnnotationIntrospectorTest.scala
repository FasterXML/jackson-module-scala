package __foursquare_shaded__.com.fasterxml.jackson
package module.scala
package introspect

import __foursquare_shaded__.com.fasterxml.jackson.annotation.JsonProperty
import __foursquare_shaded__.com.fasterxml.jackson.core.JsonGenerator
import __foursquare_shaded__.com.fasterxml.jackson.databind.module.SimpleModule
import __foursquare_shaded__.com.fasterxml.jackson.databind.ser.ContextualSerializer
import __foursquare_shaded__.com.fasterxml.jackson.databind.{BeanDescription, JsonSerializer, ObjectMapper, SerializerProvider}
import __foursquare_shaded__.com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import org.junit.runner.RunWith
import org.scalatest.LoneElement._
import org.scalatest.{Matchers, Outcome, fixture}
import org.scalatestplus.junit.JUnitRunner

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

@RunWith(classOf[JUnitRunner])
class ScalaAnnotationIntrospectorTest extends fixture.FlatSpec with Matchers {
  import ScalaAnnotationIntrospectorTest._

  type FixtureParam = ObjectMapper with ScalaObjectMapper

  override def withFixture(test: OneArgTest): Outcome = {
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
        override val handledType: Class[Token] = classOf[Token]
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
        override val handledType: Class[Token] = classOf[Token]
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

  it should "correctly infer name(s) of un-named bean properties" in { mapper =>
    val tree = mapper.valueToTree[databind.node.ObjectNode](new JavaBeanPropertyClass)
    tree.has("value") shouldBe true
    tree.has("setValue") shouldBe false
    tree.has("getValue") shouldBe false
  }

  private def getProps(mapper: ObjectMapper, bean: AnyRef) = {
    val config = mapper.getSerializationConfig
    val beanDescription: BeanDescription = config.introspect(mapper.constructType(bean.getClass))
    beanDescription.findProperties()
  }
}
