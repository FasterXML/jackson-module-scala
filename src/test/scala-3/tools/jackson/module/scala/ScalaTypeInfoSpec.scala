package tools.jackson.module.scala

import tools.jackson.databind.annotation.JsonDeserialize
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

case class Erased(aLong: Option[Long], anInt: Option[Int], aStr: Option[String],
                  longs: Seq[Long], plain: String) derives ScalaTypeInfo

// only ever used by the precedence test, so nothing has introspected it beforehand
case class ErasedPrecedence(aLong: Option[Long]) derives ScalaTypeInfo

case class Layered(twice: Option[Option[Long]], inSeq: Option[Seq[Long]], ofOption: Seq[Option[Long]],
                   mapValue: Map[String, Long], mapKey: Map[Long, String]) derives ScalaTypeInfo

case class Generic[T](value: T)

// a primitive somewhere other than the content of an Option, a collection or a Map: a tuple slot, a
// side of an Either, the argument of a generic class, a Map key - and any nesting of those
case class Shaped(tupled: Seq[(String, Long)], bare: (String, Long), either: Either[String, Long],
                  inOption: Option[Either[String, Long]], generic: Option[Generic[Long]],
                  mapKey: Map[Long, String], mapBoth: Map[Long, Long], mapKeyOfSeq: Map[Long, Seq[Long]],
                  array: Option[Array[Long]]) derives ScalaTypeInfo

// a type parameter of the class is only known from the type Jackson is asked to read, so a field that
// mentions one is left to Jackson; the other fields are still described
case class Parameterised[T](value: Option[T], keyed: Map[T, Long], aLong: Option[Long]) derives ScalaTypeInfo

// a @JsonDeserialize on the field has already said what the property is, or what it holds
case class Redirected(@JsonDeserialize(as = classOf[List[Long]]) longs: Seq[Long]) derives ScalaTypeInfo
case class Annotated(@JsonDeserialize(contentAs = classOf[Int]) aLong: Option[Long]) derives ScalaTypeInfo

// one derives on the enum covers every case
enum Shape derives ScalaTypeInfo:
  case Circle(radius: Option[Long])
  case Rect(sides: Map[Long, Long])
  case Dot

// one derives on the base covers every implementation, through a sealed trait between as well
sealed trait Vehicle derives SealedSubtypes, ScalaTypeInfo
case class Car(seats: Option[Long]) extends Vehicle
sealed trait Boat extends Vehicle
case class Yacht(berths: Seq[Long]) extends Boat
case object Raft extends Vehicle

case class Garage(vehicles: Seq[Vehicle])

// a var is set after construction, through a setter, and is described all the same
class Mutable(val plain: String) derives ScalaTypeInfo {
  var aLong: Option[Long] = None
  var byId: Map[Long, String] = Map.empty
  var aStr: Option[String] = None
  private var hidden: Option[Long] = None
  def hiddenValue: Option[Long] = hidden
}

// a var taken from a trait: Scala 3 emits its setter on the class after erasure (scala/scala3#6350)
trait HasCount { var count: Option[Long] }
class Counted extends HasCount derives ScalaTypeInfo {
  var count: Option[Long] = None
}

// a var annotated by hand is left to the annotation
class MutableAnnotated derives ScalaTypeInfo {
  @JsonDeserialize(contentAs = classOf[Int]) var aLong: Option[Long] = None
}

class ScalaTypeInfoSpec extends AnyWordSpec with Matchers with BeforeAndAfterEach {

  private val json = """{"aLong":2,"anInt":1,"aStr":"x","longs":[3],"plain":"p"}"""

  override def afterEach(): Unit = ScalaAnnotationIntrospectorModule.clearRegisteredReferencedTypes()

  private def mapper = JsonMapper.builder().addModule(DefaultScalaModule).build()

  private def fieldsOf[T](using info: ScalaTypeInfo[T]): Map[String, ScalaTypeInfo.TypeShape] =
    info.erasedFields.map { case (_, name, shape) => name -> shape }.toMap

  private def classesOf(shape: ScalaTypeInfo.TypeShape): Any =
    if (shape.typeArguments.isEmpty) shape.rawClass else (shape.rawClass, shape.typeArguments.map(classesOf))

  "ScalaTypeInfo" should {
    "capture only the fields that lose a type argument to erasure" in {
      val captured = fieldsOf[Erased]
      captured.keySet shouldEqual Set("aLong", "anInt", "longs")
      classesOf(captured("aLong")) shouldEqual (classOf[Option[?]], Seq(classOf[Long]))
      classesOf(captured("longs")) shouldEqual (classOf[Seq[?]], Seq(classOf[Long]))
      // a reference type argument survives in the generic signature, so nothing is needed for it
      captured.keySet should not contain "aStr"
      captured.keySet should not contain "plain"
    }
    "be discoverable from the companion at runtime" in {
      val companion = Class.forName(classOf[Erased].getName + "$")
      companion.getMethods.map(_.getName) should contain("derived$ScalaTypeInfo")
    }
    // deriving is the whole of it - nothing is registered by hand. Note Option[Int] would have
    // worked either way: a small JSON number is read as an Integer, which is what Int boxes to.
    // Option[Long] is where erasure bites.
    "hold a type argument the JVM erased, with nothing registered by hand" in {
      val read = mapper.readValue(json, classOf[Erased])
      read.aLong.map(_.getClass.getName) shouldEqual Some("long")
      read.aLong.map(_ + 1L) shouldEqual Some(3L)
      read.longs.map(_ + 1L) shouldEqual Seq(4L)
      read.anInt.map(_ + 1) shouldEqual Some(2)
      read.aStr shouldEqual Some("x")
      read.plain shouldEqual "p"
    }
    "describe a type argument however deeply it is nested" in {
      val captured = fieldsOf[Layered]
      classesOf(captured("twice")) shouldEqual (classOf[Option[?]], Seq((classOf[Option[?]], Seq(classOf[Long]))))
      classesOf(captured("inSeq")) shouldEqual (classOf[Option[?]], Seq((classOf[Seq[?]], Seq(classOf[Long]))))
      classesOf(captured("ofOption")) shouldEqual (classOf[Seq[?]], Seq((classOf[Option[?]], Seq(classOf[Long]))))
      classesOf(captured("mapValue")) shouldEqual (classOf[Map[?, ?]], Seq(classOf[String], classOf[Long]))
      classesOf(captured("mapKey")) shouldEqual (classOf[Map[?, ?]], Seq(classOf[Long], classOf[String]))
    }
    "read every nested shape back with nothing registered by hand" in {
      val json = """{"twice":2,"inSeq":[2],"ofOption":[2],"mapValue":{"a":2},"mapKey":{"2":"a"}}"""
      val read = mapper.readValue(json, classOf[Layered])
      read.twice.map(_.map(_ + 1L)) shouldEqual Some(Some(3L))
      read.inSeq.map(_.map(_ + 1L)) shouldEqual Some(Seq(3L))
      read.ofOption.map(_.map(_ + 1L)) shouldEqual Seq(Some(3L))
      read.mapValue.map { case (_, v) => v + 1L } shouldEqual Seq(3L)
      read.mapKey.map { case (k, _) => k + 1L } shouldEqual Seq(3L)
    }
    "read a primitive wherever it sits in the type" in {
      val json = """{"tupled":[["a",2]],"bare":["b",2],"either":{"r":2},"inOption":{"r":2},"generic":{"value":2},""" +
        """"mapKey":{"2":"a"},"mapBoth":{"2":2},"mapKeyOfSeq":{"2":[2]},"array":[2]}"""
      val read = mapper.readValue(json, classOf[Shaped])
      read.tupled.map(_._2 + 1L) shouldEqual Seq(3L)
      read.bare._2 + 1L shouldEqual 3L
      read.either.map(_ + 1L) shouldEqual Right(3L)
      read.inOption.map(_.map(_ + 1L)) shouldEqual Some(Right(3L))
      read.generic.map(_.value + 1L) shouldEqual Some(3L)
      read.mapKey.map { case (k, _) => k + 1L } shouldEqual Seq(3L)
      read.mapBoth.map { case (k, v) => k + v } shouldEqual Seq(4L)
      read.mapKeyOfSeq.map { case (k, v) => k + v.sum } shouldEqual Seq(4L)
      read.array.map(_.map(_ + 1L).toSeq) shouldEqual Some(Seq(3L))
    }
    "leave a field that mentions a type parameter of the class to Jackson" in {
      // a generic class derives a method that wants an instance for T, so it is summoned with one
      given ScalaTypeInfo[String] = ScalaTypeInfo.derivedFrom(Seq.empty, Seq.empty)
      fieldsOf[Parameterised[String]].keySet shouldEqual Set("aLong")
      val read = mapper.readValue("""{"value":"x","keyed":{"a":2},"aLong":2}""", classOf[Parameterised[String]])
      read.value shouldEqual Some("x")
      read.aLong.map(_ + 1L) shouldEqual Some(3L)
    }
    "leave a registration made by hand alone" in {
      // registered before anything introspects the class, so deriving would be the one to clobber it
      ScalaAnnotationIntrospectorModule
        .registerReferencedValueType(classOf[ErasedPrecedence], "aLong", classOf[Int])
      mapper.readValue("""{"aLong":2}""", classOf[ErasedPrecedence])
      ScalaAnnotationIntrospectorModule
        .getRegisteredReferencedValueType(classOf[ErasedPrecedence], "aLong") shouldEqual Some(classOf[Int])
    }
    "leave a property that carries a @JsonDeserialize alone" in {
      mapper.readValue("""{"longs":[2]}""", classOf[Redirected]).longs shouldBe a[List[?]]
      // the annotation names Int where deriving would have named Long; what the annotation says is
      // what is read, so the value is looked at without unboxing it
      val read = mapper.readValue("""{"aLong":2}""", classOf[Annotated])
      val held = classOf[Annotated].getMethod("aLong").invoke(read).asInstanceOf[Option[Any]]
      held.map(_.getClass) shouldEqual Some(classOf[java.lang.Integer])
    }
    "describe a var as it describes a constructor parameter" in {
      fieldsOf[Mutable].keySet shouldEqual Set("aLong", "byId")
      val read = mapper.readValue("""{"plain":"p","aLong":2,"byId":{"3":"x"},"aStr":"s","hidden":4}""", classOf[Mutable])
      read.plain shouldEqual "p"
      read.aLong.map(_ + 1L) shouldEqual Some(3L)
      read.byId.map { case (k, _) => k + 1L } shouldEqual Seq(4L)
      read.aStr shouldEqual Some("s")
    }
    "describe a var taken from a trait" in {
      fieldsOf[Counted].keySet shouldEqual Set("count")
      mapper.readValue("""{"count":2}""", classOf[Counted]).count.map(_ + 1L) shouldEqual Some(3L)
    }
    "leave a var that carries a @JsonDeserialize alone" in {
      val read = mapper.readValue("""{"aLong":2}""", classOf[MutableAnnotated])
      val held = classOf[MutableAnnotated].getMethod("aLong").invoke(read).asInstanceOf[Option[Any]]
      held.map(_.getClass) shouldEqual Some(classOf[java.lang.Integer])
    }
    "cover every case of an enum from one derives on the enum" in {
      fieldsOf[Shape].keySet shouldEqual Set("radius", "sides")
      mapper.readValue("""{"@type":"Circle","radius":2}""", classOf[Shape]) match {
        case Shape.Circle(radius) => radius.map(_ + 1L) shouldEqual Some(3L)
        case other => fail(s"read $other")
      }
      mapper.readValue("""{"@type":"Rect","sides":{"2":2}}""", classOf[Shape]) match {
        case Shape.Rect(sides) => sides.map { case (k, v) => k + v } shouldEqual Seq(4L)
        case other => fail(s"read $other")
      }
      mapper.readValue("""{"radius":2}""", classOf[Shape.Circle]).radius.map(_ + 1L) shouldEqual Some(3L)
      mapper.readValue("\"Dot\"", classOf[Shape]) shouldEqual Shape.Dot
    }
    "cover every implementation of a sealed hierarchy from one derives on the base" in {
      fieldsOf[Vehicle].keySet shouldEqual Set("seats", "berths")
      val json = """{"vehicles":[{"@type":"Car","seats":2},{"@type":"Yacht","berths":[2]},{"@type":"Raft"}]}"""
      val read = mapper.readValue(json, classOf[Garage])
      read.vehicles.collect { case Car(seats) => seats.map(_ + 1L) } shouldEqual Seq(Some(3L))
      read.vehicles.collect { case Yacht(berths) => berths.map(_ + 1L) } shouldEqual Seq(Seq(3L))
      read.vehicles should contain(Raft)
      mapper.readValue("""{"seats":2}""", classOf[Car]).seats.map(_ + 1L) shouldEqual Some(3L)
    }
  }
}
