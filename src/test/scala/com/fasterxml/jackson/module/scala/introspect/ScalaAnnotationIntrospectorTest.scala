package com.fasterxml.jackson
package module.scala
package introspect

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.ContextualSerializer
import com.fasterxml.jackson.databind.util.LookupCache
import com.fasterxml.jackson.databind.{BeanDescription, JsonSerializer, MapperFeature, ObjectMapper, SerializerProvider}
import com.fasterxml.jackson.module.scala.deser.OptionWithNumberDeserializerTest.OptionLong
import com.fasterxml.jackson.module.scala.deser.ValueHolder
import org.scalatest.LoneElement.convertToCollectionLoneElementWrapper
import org.scalatest.Outcome
import org.scalatest.flatspec.FixtureAnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.beans.BeanProperty
import scala.collection.JavaConverters._
import scala.collection.concurrent.TrieMap

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
  class GeneratedDefaultArgumentClass {
    def getValue(value: String = "default"): String = value
  }

  case class CaseClassWithDefault(a: String = "defaultParam", b: Option[String] = Some("optionDefault"), c: Option[String])

  class ConcurrentLookupCache[K, V]() extends LookupCache[K, V] {
    final private val cache = TrieMap.empty[K, V]

    override def put(key: K, value: V): V =
      cache.put(key, value).getOrElse(None.orNull).asInstanceOf[V]

    override def putIfAbsent(key: K, value: V): V =
      cache.putIfAbsent(key, value).getOrElse(None.orNull).asInstanceOf[V]

    override def get(key: Any): V = {
      cache.get(key.asInstanceOf[K]).getOrElse(None.orNull).asInstanceOf[V]
    }

    override def clear(): Unit = {
      cache.clear()
    }

    override def size: Int = cache.size
  }

  object ConcurrentLookupCacheFactory extends LookupCacheFactory {
    override def createLookupCache[K, V](initialEntries: Int, maxEntries: Int): LookupCache[K, V] =
      new ConcurrentLookupCache[K, V]
  }
}

class ScalaAnnotationIntrospectorTest extends FixtureAnyFlatSpec with Matchers {
  import ScalaAnnotationIntrospectorTest._

  type FixtureParam = ObjectMapper

  override def withFixture(test: OneArgTest): Outcome = {
    val mapper = new ObjectMapper
    mapper.registerModule(DefaultScalaModule)
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
    allProps.loneElement should have (Symbol("name") ("param"))

    val prop = allProps.asScala.head
    prop should have (
      Symbol("hasField") (true),
      Symbol("hasGetter") (true),
      Symbol("hasConstructorParameter") (true)
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

  it should "respect APPLY_DEFAULT_VALUES true" in { _ =>

    val builder = JsonMapper.builder().enable(MapperFeature.APPLY_DEFAULT_VALUES).addModule(DefaultScalaModule)
    val mapper = builder.build()

    val json = """
        |{}
        |""".stripMargin

    val jsonWithKey = """
                 |{"a": "notDefault"}
                 |""".stripMargin

    val jsonWithNulls = """
                          |{"a": null, "b": null, "c": null}
                          |""".stripMargin

    val withDefault = mapper.readValue(json, classOf[CaseClassWithDefault])
    val withoutDefault = mapper.readValue(jsonWithKey, classOf[CaseClassWithDefault])
    val withNulls = mapper.readValue(jsonWithNulls, classOf[CaseClassWithDefault])


    withDefault.a shouldBe "defaultParam"

    withoutDefault.a shouldBe "notDefault"

    withNulls.a shouldBe "defaultParam"
    withNulls.b shouldBe Some("optionDefault")
    withNulls.c shouldBe None
  }

  it should "respect APPLY_DEFAULT_VALUES false" in { _ =>
    val builder = JsonMapper.builder().disable(MapperFeature.APPLY_DEFAULT_VALUES).addModule(DefaultScalaModule)
    val mapper = builder.build()

    val json = """
                 |{}
                 |""".stripMargin

    val jsonWithKey = """
                        |{"a": "notDefault"}
                        |""".stripMargin

    val jsonWithNulls = """
                        |{"a": null, "b": null, "c": null}
                        |""".stripMargin

    val withDefault = mapper.readValue(json, classOf[CaseClassWithDefault])
    val withoutDefault = mapper.readValue(jsonWithKey, classOf[CaseClassWithDefault])
    val withNulls = mapper.readValue(jsonWithNulls, classOf[CaseClassWithDefault])

    withDefault.a shouldBe null

    withoutDefault.a shouldBe "notDefault"

    withNulls.a shouldBe null
    withNulls.b shouldBe None
    withNulls.c shouldBe None
  }

  it should "register and retrieve reference type override" in { _ =>
    val caseClass = classOf[OptionLong]
    val refClass = classOf[Long]
    ScalaAnnotationIntrospectorModule.getRegisteredReferencedValueType(caseClass, "value") shouldBe empty
    try {
      ScalaAnnotationIntrospectorModule.registerReferencedValueType(caseClass, "value", refClass)
      ScalaAnnotationIntrospectorModule.getRegisteredReferencedValueType(caseClass, "value") shouldEqual Some(refClass)
    } finally {
      ScalaAnnotationIntrospectorModule.clearRegisteredReferencedTypes(caseClass)
      ScalaAnnotationIntrospectorModule.getRegisteredReferencedValueType(caseClass, "value") shouldBe empty
    }
  }

  it should "allow descriptor cache to be replaced (old style)" in { _ =>
    val defaultCache = ScalaAnnotationIntrospectorModule._descriptorCache
    try {
      val cache = new ConcurrentLookupCache[String, BeanDescriptor]()
      ScalaAnnotationIntrospectorModule.setDescriptorCache(cache)
      val builder = JsonMapper.builder().addModule(DefaultScalaModule)
      val mapper = builder.build()
      val jsonWithKey = """{"a": "notDefault"}"""

      val withoutDefault = mapper.readValue(jsonWithKey, classOf[CaseClassWithDefault])
      withoutDefault.a shouldEqual "notDefault"

      cache.size shouldBe >=(1)
      cache.get(classOf[CaseClassWithDefault].getName) should not be (null)
    } finally {
      ScalaAnnotationIntrospectorModule.setDescriptorCache(defaultCache)
    }
  }

  it should "allow descriptor cache to be replaced (new style)" in { _ =>
    val defaultCache = ScalaAnnotationIntrospectorModule._descriptorCache
    try {
      ScalaAnnotationIntrospectorModule.setLookupCacheFactory(ConcurrentLookupCacheFactory)
      val builder = JsonMapper.builder().addModule(DefaultScalaModule)
      val mapper = builder.build()
      val jsonWithKey = """{"a": "notDefault"}"""

      val withoutDefault = mapper.readValue(jsonWithKey, classOf[CaseClassWithDefault])
      withoutDefault.a shouldEqual "notDefault"

      ScalaAnnotationIntrospectorModule._descriptorCache shouldBe a[ConcurrentLookupCache[String, BeanDescriptor]]
      val cache = ScalaAnnotationIntrospectorModule._descriptorCache
        .asInstanceOf[ConcurrentLookupCache[String, BeanDescriptor]]
      cache.size shouldBe >=(1)
      cache.get(classOf[CaseClassWithDefault].getName) should not be (null)
      defaultCache.size() shouldEqual 0
    } finally {
      ScalaAnnotationIntrospectorModule.setLookupCacheFactory(DefaultLookupCacheFactory)
    }
  }

  it should "allow scala type cache to be replaced (old style)" in { _ =>
    val defaultCache = ScalaAnnotationIntrospectorModule._scalaTypeCache
    try {
      val cache = new ConcurrentLookupCache[String, Boolean]()
      ScalaAnnotationIntrospectorModule.setScalaTypeCache(cache)
      val builder = JsonMapper.builder().addModule(DefaultScalaModule)
      val mapper = builder.build()
      val jsonWithKey = """{"a": "notDefault"}"""

      val withoutDefault = mapper.readValue(jsonWithKey, classOf[CaseClassWithDefault])
      withoutDefault.a shouldEqual "notDefault"

      cache.size shouldBe >=(1)
      cache.get(classOf[CaseClassWithDefault].getName) shouldBe true

      val javaValueHolder = mapper.readValue("\"2\"", classOf[ValueHolder])
      javaValueHolder should not be(null)
      cache.get(classOf[ValueHolder].getName) shouldBe false
    } finally {
      ScalaAnnotationIntrospectorModule.setScalaTypeCache(defaultCache)
    }
  }

  it should "allow scala type cache to be replaced (new style)" in { _ =>
    val defaultCache = ScalaAnnotationIntrospectorModule._scalaTypeCache
    try {
      ScalaAnnotationIntrospectorModule.setLookupCacheFactory(ConcurrentLookupCacheFactory)
      val builder = JsonMapper.builder().addModule(DefaultScalaModule)
      val mapper = builder.build()
      val jsonWithKey = """{"a": "notDefault"}"""

      val withoutDefault = mapper.readValue(jsonWithKey, classOf[CaseClassWithDefault])
      withoutDefault.a shouldEqual "notDefault"

      ScalaAnnotationIntrospectorModule._scalaTypeCache shouldBe a[ConcurrentLookupCache[String, Boolean]]
      val cache = ScalaAnnotationIntrospectorModule._scalaTypeCache
        .asInstanceOf[ConcurrentLookupCache[String, Boolean]]
      cache.size shouldBe >=(1)
      cache.get(classOf[CaseClassWithDefault].getName) shouldBe true

      val javaValueHolder = mapper.readValue("\"2\"", classOf[ValueHolder])
      javaValueHolder should not be (null)
      cache.get(classOf[ValueHolder].getName) shouldBe false

      defaultCache.size() shouldEqual 0
    } finally {
      ScalaAnnotationIntrospectorModule.setLookupCacheFactory(DefaultLookupCacheFactory)
    }
  }

  it should "allow scala3 check to be disabled" in { _ =>
    ScalaAnnotationIntrospectorModule.shouldSupportScala3Classes() shouldBe true
    try {
      ScalaAnnotationIntrospectorModule.supportScala3Classes(false)
      ScalaAnnotationIntrospectorModule.shouldSupportScala3Classes() shouldBe false
    } finally {
      ScalaAnnotationIntrospectorModule.supportScala3Classes(true)
    }
  }

  it should "ignore a generated default argument method" in { mapper =>
    val bean = new GeneratedDefaultArgumentClass
    val allProps = getProps(mapper, bean)
    allProps shouldBe empty
  }

  private def getProps(mapper: ObjectMapper, bean: AnyRef) = {
    val config = mapper.getSerializationConfig
    val beanDescription: BeanDescription = config.introspect(mapper.constructType(bean.getClass))
    beanDescription.findProperties()
  }
}
