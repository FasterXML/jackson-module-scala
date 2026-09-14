package tools.jackson.module.scala.deser

import com.fasterxml.jackson.annotation.JsonTypeInfo
import tools.jackson.databind.DefaultTyping
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import tools.jackson.module.scala.DefaultScalaModule

import scala.collection.{immutable, mutable}

object DefaultTypingMapDeserializerTest {
  case class HasMap(m: Map[String, String])
  case class HasCollectionMap(m: collection.Map[String, String])
  case class HasMutableMap(m: mutable.Map[String, String])
  case class HasSortedMap(m: collection.SortedMap[String, String])
  case class HasListMap(m: immutable.ListMap[String, String])
  case class HasAnyMap(m: Map[String, Any])
  case class HasNestedMap(m: Map[String, Map[String, Int]])
  case class Inner(i: Int)
  case class HasBeanMap(m: Map[String, Inner])
}

// Type ids used to name the java.util.Map wrapper the Scala Map is serialized through, which is
// not a Scala Map, so nothing written with polymorphic typing enabled could be read back (#643)
class DefaultTypingMapDeserializerTest extends DeserializerTest {
  import DefaultTypingMapDeserializerTest._

  def module: DefaultScalaModule.type = DefaultScalaModule

  private def mapperWith(typing: DefaultTyping, as: JsonTypeInfo.As): JsonMapper = {
    val ptv = BasicPolymorphicTypeValidator.builder().allowIfBaseType(classOf[Any]).build()
    newBuilder.activateDefaultTyping(ptv, typing, as).build()
  }

  private def roundtrip[T <: AnyRef](mapper: JsonMapper, value: T, cls: Class[T]): Unit = {
    val json = mapper.writeValueAsString(value)
    withClue(json) {
      json should not include "JavaCollectionWrappers"
      mapper.readValue(json, cls) shouldEqual value
    }
  }

  private val shapes = for {
    typing <- Seq(DefaultTyping.NON_FINAL, DefaultTyping.OBJECT_AND_NON_CONCRETE)
    as <- Seq(JsonTypeInfo.As.WRAPPER_ARRAY, JsonTypeInfo.As.PROPERTY, JsonTypeInfo.As.WRAPPER_OBJECT)
  } yield (typing, as)

  for ((typing, as) <- shapes) {
    val mapper = mapperWith(typing, as)

    "Scala Module" should s"roundtrip immutable Map with default typing $typing/$as" in {
      roundtrip(mapper, HasMap(immutable.Map("one" -> "one", "two" -> "two")), classOf[HasMap])
    }

    it should s"roundtrip empty and single-entry immutable Maps with default typing $typing/$as" in {
      roundtrip(mapper, HasMap(immutable.Map.empty), classOf[HasMap])
      roundtrip(mapper, HasMap(immutable.Map("one" -> "one")), classOf[HasMap])
    }

    it should s"roundtrip large immutable Map (HashMap) with default typing $typing/$as" in {
      roundtrip(mapper, HasMap((1 to 20).map(i => s"k$i" -> s"v$i").toMap), classOf[HasMap])
    }

    it should s"roundtrip collection.Map with default typing $typing/$as" in {
      roundtrip(mapper, HasCollectionMap(immutable.Map("one" -> "one", "two" -> "two")), classOf[HasCollectionMap])
    }

    it should s"roundtrip mutable Map with default typing $typing/$as" in {
      roundtrip(mapper, HasMutableMap(mutable.Map("one" -> "one", "two" -> "two")), classOf[HasMutableMap])
      roundtrip(mapper, HasMutableMap(mutable.LinkedHashMap("one" -> "one", "two" -> "two")), classOf[HasMutableMap])
    }

    it should s"roundtrip SortedMap with default typing $typing/$as" in {
      roundtrip(mapper, HasSortedMap(immutable.TreeMap("b" -> "2", "a" -> "1")), classOf[HasSortedMap])
    }

    it should s"roundtrip ListMap with default typing $typing/$as" in {
      roundtrip(mapper, HasListMap(immutable.ListMap("b" -> "2", "a" -> "1")), classOf[HasListMap])
    }

    it should s"roundtrip Map with polymorphic values with default typing $typing/$as" in {
      roundtrip(mapper, HasAnyMap(immutable.Map("s" -> "str", "i" -> 1, "m" -> Map("x" -> "y"))), classOf[HasAnyMap])
      roundtrip(mapper, HasNestedMap(immutable.Map("a" -> Map("x" -> 1), "b" -> Map.empty)), classOf[HasNestedMap])
      roundtrip(mapper, HasBeanMap(immutable.Map("a" -> Inner(1))), classOf[HasBeanMap])
    }

    // the runtime class of a root value is final (Map$Map2), so as with any final root value the
    // declared type must be given for a type id to be written at all
    it should s"roundtrip a top-level Map with default typing $typing/$as" in {
      val value = immutable.Map("one" -> "one", "two" -> "two")
      val json = mapper.writerFor(classOf[Map[String, String]]).writeValueAsString(value)
      withClue(json) {
        json should not include "JavaCollectionWrappers"
        mapper.readValue(json, classOf[Map[String, String]]) shouldEqual value
      }
    }
  }
}
