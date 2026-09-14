package tools.jackson.module.scala.deser

import com.fasterxml.jackson.annotation.JsonTypeInfo
import tools.jackson.core.`type`.TypeReference
import tools.jackson.databind.DefaultTyping
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator
// qualified: this package has fixtures of its own by some of these names
import tools.jackson.module.scala.poly
import tools.jackson.module.scala.ser.EnumerationSerializerTest.{AnnotationHolder, WeekdayType}
import tools.jackson.module.scala.{DefaultScalaModule, JsonScalaEnumeration, Weekday}

import scala.collection.{immutable, mutable}
import scala.concurrent.duration.{Duration, FiniteDuration}

object DefaultTypingRoundtripTest {
  case class Inner(i: Int)

  case class HasEither(e: Either[String, Int])
  case class HasEitherOfBeanOrList(e: Either[Inner, List[Int]])
  case class HasOptionEither(o: Option[Either[String, Inner]])
  case class HasListOfEither(l: List[Either[String, Int]])

  case class HasTuple(t: (String, Int))
  case class HasTupleOfAny(t: (Any, Any))
  case class HasTupleOfBeanAndList(t: (Inner, List[Int]))
  case class HasTuple3(t: (String, Inner, Option[Int]))

  case class HasEnumeration(w: Weekday.Weekday)
  case class HasEnumerationSet(@JsonScalaEnumeration(classOf[WeekdayType]) days: Set[Weekday.Value])

  case class HasDuration(d: Duration)
  case class HasFiniteDuration(d: FiniteDuration)

  case class HasIterator(it: Iterator[Int])

  case class HasOptionAny(o: Option[Any])
}

// Every Scala type the module serializes itself, written and read back with default typing enabled,
// over both applicability modes and every inclusion shape. Map and Iterable have their own tests.
class DefaultTypingRoundtripTest extends DeserializerTest {
  import DefaultTypingRoundtripTest._

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

  // the exact shapes, once, for the cases that used to come out wrong
  "Scala Module" should "write an Either as one object under its type id, and type its content by the content's type" in {
    val mapper = mapperWith(DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY)
    mapper.writeValueAsString(HasEither(Left("x"))) shouldEqual
      """{"@class":"tools.jackson.module.scala.deser.DefaultTypingRoundtripTest$HasEither","e":{"@class":"scala.util.Left","l":"x"}}"""
    mapper.writeValueAsString(HasEitherOfBeanOrList(Left(Inner(1)))) shouldEqual
      """{"@class":"tools.jackson.module.scala.deser.DefaultTypingRoundtripTest$HasEitherOfBeanOrList","e":{"@class":"scala.util.Left","l":{"@class":"tools.jackson.module.scala.deser.DefaultTypingRoundtripTest$Inner","i":1}}}"""
  }

  it should "write a Tuple as one array under its type id, typing elements by their declared type" in {
    val mapper = mapperWith(DefaultTyping.OBJECT_AND_NON_CONCRETE, JsonTypeInfo.As.WRAPPER_ARRAY)
    mapper.writeValueAsString(HasTuple(("a", 1))) shouldEqual """{"t":["a",1]}"""
    mapper.writeValueAsString(HasTupleOfAny((Inner(1), "s"))) shouldEqual
      """{"t":[["tools.jackson.module.scala.deser.DefaultTypingRoundtripTest$Inner",{"i":1}],"s"]}"""
    mapperWith(DefaultTyping.NON_FINAL, JsonTypeInfo.As.WRAPPER_ARRAY).writeValueAsString(HasTuple(("a", 1))) shouldEqual
      """["tools.jackson.module.scala.deser.DefaultTypingRoundtripTest$HasTuple",{"t":["scala.Tuple2",["a",1]]}]"""
  }

  it should "write an Iterator as one array under its type id" in {
    val mapper = mapperWith(DefaultTyping.OBJECT_AND_NON_CONCRETE, JsonTypeInfo.As.WRAPPER_ARRAY)
    val json = mapper.writeValueAsString(HasIterator(Iterator(1, 2)))
    json should startWith("""{"it":["scala.collection.""")
    json should endWith(""",[1,2]]}""")
  }

  it should "write a SealedPolymorphismSupport value under its type id without also tagging it" in {
    val mapper = mapperWith(DefaultTyping.OBJECT_AND_NON_CONCRETE, JsonTypeInfo.As.PROPERTY)
    mapper.writeValueAsString(poly.Owner("n", poly.Dog("d"))) shouldEqual
      """{"name":"n","pet":{"@class":"tools.jackson.module.scala.poly.Dog","name":"d"}}"""
    mapper.writeValueAsString(poly.Owner("n", poly.Unknown)) shouldEqual
      """{"name":"n","pet":{"@class":"tools.jackson.module.scala.poly.Unknown$"}}"""
    // and the tag as before where no type id is in play
    newMapper.writeValueAsString(poly.Owner("n", poly.Dog("d"))) shouldEqual """{"name":"n","pet":{"@type":"Dog","name":"d"}}"""
  }

  it should "write a root-level Scala collection with the type id its abstract type gets, without writerFor" in {
    val mapper = mapperWith(DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY)
    mapper.writeValueAsString(Map("a" -> "b")) shouldEqual """{"@class":"scala.collection.immutable.Map$Map1","a":"b"}"""
    mapper.writeValueAsString(List(1, 2)) shouldEqual """["scala.collection.immutable.$colon$colon",[1,2]]"""
    mapper.writeValueAsString(Map("k" -> List(1))) shouldEqual
      """{"@class":"scala.collection.immutable.Map$Map1","k":["scala.collection.immutable.$colon$colon",[1]]}"""
  }

  it should "leave a root-level Scala collection untyped when default typing would not type its abstract type" in {
    val mapper = mapperWith(DefaultTyping.JAVA_LANG_OBJECT, JsonTypeInfo.As.PROPERTY)
    mapper.writeValueAsString(Map("a" -> "b")) shouldEqual """{"a":"b"}"""
    mapper.writeValueAsString(List(1, 2)) shouldEqual """[1,2]"""
    newMapper.writeValueAsString(Map("a" -> "b")) shouldEqual """{"a":"b"}"""
  }

  for ((typing, as) <- shapes) {
    val mapper = mapperWith(typing, as)

    it should s"roundtrip Either with default typing $typing/$as" in {
      roundtrip(mapper, HasEither(Left("x")), classOf[HasEither])
      roundtrip(mapper, HasEither(Right(1)), classOf[HasEither])
      roundtrip(mapper, HasEitherOfBeanOrList(Left(Inner(1))), classOf[HasEitherOfBeanOrList])
      roundtrip(mapper, HasEitherOfBeanOrList(Right(List(1, 2))), classOf[HasEitherOfBeanOrList])
      roundtrip(mapper, HasOptionEither(Some(Right(Inner(2)))), classOf[HasOptionEither])
      roundtrip(mapper, HasOptionEither(None), classOf[HasOptionEither])
      roundtrip(mapper, HasListOfEither(List(Left("a"), Right(1))), classOf[HasListOfEither])
    }

    it should s"roundtrip Tuple with default typing $typing/$as" in {
      roundtrip(mapper, HasTuple(("a", 1)), classOf[HasTuple])
      roundtrip(mapper, HasTupleOfAny((Inner(1), List(1))), classOf[HasTupleOfAny])
      roundtrip(mapper, HasTupleOfAny(("s", 2)), classOf[HasTupleOfAny])
      roundtrip(mapper, HasTupleOfBeanAndList((Inner(1), List(1))), classOf[HasTupleOfBeanAndList])
      roundtrip(mapper, HasTuple3(("a", Inner(1), Some(2))), classOf[HasTuple3])
    }

    it should s"roundtrip Enumeration with default typing $typing/$as" in {
      roundtrip(mapper, HasEnumeration(Weekday.Fri), classOf[HasEnumeration])
      roundtrip(mapper, AnnotationHolder(Weekday.Fri), classOf[AnnotationHolder])
      roundtrip(mapper, HasEnumerationSet(Set(Weekday.Mon, Weekday.Fri)), classOf[HasEnumerationSet])
    }

    it should s"roundtrip Duration with default typing $typing/$as" in {
      roundtrip(mapper, HasDuration(Duration("5s")), classOf[HasDuration])
      roundtrip(mapper, HasFiniteDuration(FiniteDuration(5, "s")), classOf[HasFiniteDuration])
    }

    it should s"roundtrip Option[Any] holding scalars, beans and collections with default typing $typing/$as" in {
      roundtrip(mapper, HasOptionAny(Some("str")), classOf[HasOptionAny])
      roundtrip(mapper, HasOptionAny(Some(5)), classOf[HasOptionAny])
      roundtrip(mapper, HasOptionAny(Some(true)), classOf[HasOptionAny])
      roundtrip(mapper, HasOptionAny(Some(Inner(1))), classOf[HasOptionAny])
      roundtrip(mapper, HasOptionAny(Some(List(1))), classOf[HasOptionAny])
      roundtrip(mapper, HasOptionAny(Some(Map("k" -> "v"))), classOf[HasOptionAny])
      roundtrip(mapper, HasOptionAny(None), classOf[HasOptionAny])
    }

    it should s"roundtrip SealedPolymorphismSupport hierarchies with default typing $typing/$as" in {
      roundtrip(mapper, poly.Owner("n", poly.Dog("d")), classOf[poly.Owner])
      roundtrip(mapper, poly.Owner("n", poly.Unknown), classOf[poly.Owner])
      roundtrip(mapper, poly.Shelter(Seq(poly.Dog("d"), poly.Bird("b", canFly = true), poly.Unknown)), classOf[poly.Shelter])
      roundtrip(mapper, poly.Drawing(poly.Rect(1.0, 2.0)), classOf[poly.Drawing])
      roundtrip(mapper, poly.Drawing(poly.Point), classOf[poly.Drawing])
      val root: poly.Animal = poly.Dog("d")
      mapper.readValue(mapper.writeValueAsString(root), classOf[poly.Animal]) shouldEqual root
      mapper.readValue(mapper.writeValueAsString(poly.Unknown), classOf[poly.Animal]) shouldEqual poly.Unknown
    }

    it should s"roundtrip root-level Scala collections without writerFor with default typing $typing/$as" in {
      val m = Map("a" -> "b")
      mapper.readValue(mapper.writeValueAsString(m), classOf[Map[String, String]]) shouldEqual m
      val l: Seq[Int] = List(1, 2)
      mapper.readValue(mapper.writeValueAsString(l), classOf[Seq[Int]]) shouldEqual l
      val s = Set("x")
      mapper.readValue(mapper.writeValueAsString(s), classOf[Set[String]]) shouldEqual s
      // read back as the abstract type: that is the type whose typing a root collection now gets, and
      // Vector is a final class on Scala 2.12, so declared as itself it would not look for the id
      val v: Seq[Int] = Vector(1)
      mapper.readValue(mapper.writeValueAsString(v), classOf[Seq[Int]]) shouldEqual v
      val big = (1 to 5).map(i => s"k$i" -> i).toMap
      mapper.readValue(mapper.writeValueAsString(big), classOf[Map[String, Int]]) shouldEqual big
      val mm = mutable.Map("a" -> 1)
      mapper.readValue(mapper.writeValueAsString(mm), classOf[mutable.Map[String, Int]]) shouldEqual mm
      val sorted = immutable.TreeMap("b" -> 2, "a" -> 1)
      val sortedType = new TypeReference[collection.SortedMap[String, Int]] {}
      mapper.readValue(mapper.writeValueAsString(sorted), sortedType) shouldEqual sorted
      val nested = Map("k" -> List(Inner(1)))
      mapper.readValue(mapper.writeValueAsString(nested), classOf[Map[String, List[Inner]]]) shouldEqual nested
    }
  }
}
