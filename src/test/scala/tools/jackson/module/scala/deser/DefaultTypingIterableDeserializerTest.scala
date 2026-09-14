package tools.jackson.module.scala.deser

import com.fasterxml.jackson.annotation.JsonTypeInfo
import tools.jackson.databind.DefaultTyping
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import tools.jackson.module.scala.DefaultScalaModule

object DefaultTypingIterableDeserializerTest {
  case class HasAny(a: Any)
  case class HasSeq(s: Seq[Int])
  case class HasSet(s: Set[String])
  case class HasIterable(i: Iterable[String])
  case class Inner(i: Int)
  case class HasBeanList(l: List[Inner])
}

// IterableSerializer wrote a second array inside the one the type prefix had already opened, so a
// List written with polymorphic typing enabled came out as ["...$colon$colon",[[1,2]]]
class DefaultTypingIterableDeserializerTest extends DeserializerTest {
  import DefaultTypingIterableDeserializerTest._

  def module: DefaultScalaModule.type = DefaultScalaModule

  private def mapperWith(typing: DefaultTyping, as: JsonTypeInfo.As): JsonMapper = {
    val ptv = BasicPolymorphicTypeValidator.builder().allowIfBaseType(classOf[Any]).build()
    newBuilder.activateDefaultTyping(ptv, typing, as).build()
  }

  private def roundtrip[T <: AnyRef](mapper: JsonMapper, value: T, cls: Class[T]): Unit = {
    val json = mapper.writeValueAsString(value)
    withClue(json) {
      mapper.readValue(json, cls) shouldEqual value
    }
  }

  private val shapes = for {
    typing <- Seq(DefaultTyping.NON_FINAL, DefaultTyping.OBJECT_AND_NON_CONCRETE)
    as <- Seq(JsonTypeInfo.As.WRAPPER_ARRAY, JsonTypeInfo.As.PROPERTY, JsonTypeInfo.As.WRAPPER_OBJECT)
  } yield (typing, as)

  "Scala Module" should "write a List held as Any as a single array under the type id" in {
    val mapper = mapperWith(DefaultTyping.OBJECT_AND_NON_CONCRETE, JsonTypeInfo.As.WRAPPER_ARRAY)
    mapper.writeValueAsString(HasAny(List(1, 2))) shouldEqual """{"a":["scala.collection.immutable.$colon$colon",[1,2]]}"""
  }

  for ((typing, as) <- shapes) {
    val mapper = mapperWith(typing, as)

    "Scala Module" should s"roundtrip List held as Any with default typing $typing/$as" in {
      roundtrip(mapper, HasAny(List(1, 2)), classOf[HasAny])
      roundtrip(mapper, HasAny(List.empty), classOf[HasAny])
    }

    it should s"roundtrip Seq, Set and Iterable with default typing $typing/$as" in {
      roundtrip(mapper, HasSeq(Seq(1, 2, 3)), classOf[HasSeq])
      roundtrip(mapper, HasSeq(Vector(1, 2, 3)), classOf[HasSeq])
      roundtrip(mapper, HasSet(Set("a", "b")), classOf[HasSet])
      roundtrip(mapper, HasIterable(List("a", "b")), classOf[HasIterable])
    }

    it should s"roundtrip List of beans with default typing $typing/$as" in {
      roundtrip(mapper, HasBeanList(List(Inner(1), Inner(2))), classOf[HasBeanList])
    }

    it should s"roundtrip a top-level Seq with default typing $typing/$as" in {
      val value: Seq[Int] = Seq(1, 2, 3)
      val json = mapper.writerFor(classOf[Seq[Int]]).writeValueAsString(value)
      withClue(json) {
        mapper.readValue(json, classOf[Seq[Int]]) shouldEqual value
      }
    }
  }
}
