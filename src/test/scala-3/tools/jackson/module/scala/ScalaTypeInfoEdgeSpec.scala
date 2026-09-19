package tools.jackson.module.scala

import com.fasterxml.jackson.annotation.JsonCreator.Mode
import com.fasterxml.jackson.annotation.{JsonCreator, JsonProperty}
import tools.jackson.databind.MapperFeature
import tools.jackson.databind.annotation.JsonDeserialize
import tools.jackson.databind.json.JsonMapper
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.annotation.meta.field
import scala.collection.immutable.{IntMap, LongMap}
import scala.util.Try

// a default is applied by the value instantiator, after the creator property has been typed
case class Defaulted(aLong: Option[Long] = None, byId: Map[Long, String] = Map.empty, count: Long = 7) derives ScalaTypeInfo

// the annotation is put on the field rather than the constructor parameter; Jackson merges the
// two, so the parameter still counts as annotated
case class FieldAnnotated(@(JsonDeserialize @field)(contentAs = classOf[Int]) aLong: Option[Long]) derives ScalaTypeInfo

// maps the type modifier already keys by a primitive, with a value that erases
case class Specialised(ints: IntMap[Long], longs: LongMap[Long], mutableLongs: scala.collection.mutable.LongMap[Long]) derives ScalaTypeInfo

object Ids {
  opaque type UserId = Long
  object UserId {
    def apply(value: Long): UserId = value
  }
  extension (id: UserId) def value: Long = id

  // inside the scope that defines it, an opaque type is its underlying type
  case class Inside(id: Option[UserId]) derives ScalaTypeInfo
}

// outside that scope the compiler cannot see through it, so the field is not described
case class Outside(id: Option[Ids.UserId]) derives ScalaTypeInfo
case class OutsidePlain(id: Option[Ids.UserId])

// read through a companion creator: its parameters are the factory's, not the constructor's, and
// are described from the factory - by position, under whatever name @JsonProperty gives them
case class Built private (label: String, count: Option[Long]) derives ScalaTypeInfo
object Built {
  @JsonCreator(mode = Mode.PROPERTIES)
  def build(text: String, @JsonProperty("n") aLong: Option[Long], byId: Map[Long, String]): Built =
    Built(text.toUpperCase + byId.keys.sum, aLong)
}

// creators told apart by how many parameters they take
case class Overloaded private (value: Option[Long]) derives ScalaTypeInfo
object Overloaded {
  @JsonCreator(mode = Mode.DELEGATING)
  def apply(value: Long): Overloaded = new Overloaded(Some(value))
  @JsonCreator(mode = Mode.PROPERTIES)
  def apply(value: Option[Long], scale: Int): Overloaded = new Overloaded(value.map(_ * scale))
}

class ScalaTypeInfoEdgeSpec extends AnyWordSpec with Matchers {

  private def mapper = JsonMapper.builder().addModule(DefaultScalaModule).build()

  private def fieldsOf[T](using info: ScalaTypeInfo[T]): Set[String] = info.erasedFields.map(_._2).toSet

  // how a read ends, so that a class can be compared with its underived twin
  private def outcome[T](json: String, clazz: Class[T], use: T => Any): Either[String, Any] =
    Try(use(mapper.readValue(json, clazz))).toEither.left.map(_.getClass.getSimpleName)

  "ScalaTypeInfo" should {
    "type a parameter with a default as it types any other" in {
      fieldsOf[Defaulted] shouldEqual Set("aLong", "byId")
      val defaulted = mapper.readValue("{}", classOf[Defaulted])
      defaulted shouldEqual Defaulted()
      val supplied = mapper.readValue("""{"aLong":2,"byId":{"3":"x"},"count":4}""", classOf[Defaulted])
      supplied.aLong.map(_ + 1L) shouldEqual Some(3L)
      supplied.byId.map { case (k, _) => k + 1L } shouldEqual Seq(4L)
      supplied.count shouldEqual 4L
    }
    "type a parameter with a default when defaults are not applied" in {
      val noDefaults = JsonMapper.builder().addModule(DefaultScalaModule)
        .disable(MapperFeature.APPLY_DEFAULT_VALUES).build()
      noDefaults.readValue("""{"aLong":2}""", classOf[Defaulted]).aLong.map(_ + 1L) shouldEqual Some(3L)
    }
    "leave a parameter whose field carries the @JsonDeserialize alone" in {
      val read = mapper.readValue("""{"aLong":2}""", classOf[FieldAnnotated])
      val held = classOf[FieldAnnotated].getMethod("aLong").invoke(read).asInstanceOf[Option[Any]]
      held.map(_.getClass) shouldEqual Some(classOf[java.lang.Integer])
    }
    "describe the value of a map the module keys by a primitive itself" in {
      fieldsOf[Specialised] shouldEqual Set("ints", "longs", "mutableLongs")
      val read = mapper.readValue("""{"ints":{"1":2},"longs":{"3":4},"mutableLongs":{"5":6}}""", classOf[Specialised])
      read.ints.map { case (k, v) => k + v } shouldEqual Seq(3L)
      read.longs.map { case (k, v) => k + v } shouldEqual Seq(7L)
      read.mutableLongs.map { case (k, v) => k + v } shouldEqual Seq(11L)
    }
    "see through an opaque type derived inside its scope" in {
      fieldsOf[Ids.Inside] shouldEqual Set("id")
      mapper.readValue("""{"id":2}""", classOf[Ids.Inside]).id.map(_.value + 1L) shouldEqual Some(3L)
    }
    "not see through an opaque type derived outside its scope, and read it as without the derives" in {
      fieldsOf[Outside] shouldBe empty
      val derived = outcome("""{"id":2}""", classOf[Outside], _.id.map(_.value + 1L))
      val plain = outcome("""{"id":2}""", classOf[OutsidePlain], _.id.map(_.value + 1L))
      derived shouldEqual plain
      derived shouldEqual Left("ClassCastException")
    }
    "describe the parameters of a companion creator" in {
      val captured = summon[ScalaTypeInfo[Built]].erasedCreatorParameters.map(_._1)
      captured shouldEqual Seq(
        ScalaTypeInfo.CreatorParameter(classOf[Built], "build", 3, 1),
        ScalaTypeInfo.CreatorParameter(classOf[Built], "build", 3, 2))
      val read = mapper.readValue("""{"text":"x","n":2,"byId":{"3":"a"}}""", classOf[Built])
      read.label shouldEqual "X3"
      read.count.map(_ + 1L) shouldEqual Some(3L)
    }
    "tell overloaded companion creators apart by their arity" in {
      val captured = summon[ScalaTypeInfo[Overloaded]].erasedCreatorParameters.map(_._1)
      captured shouldEqual Seq(ScalaTypeInfo.CreatorParameter(classOf[Overloaded], "apply", 2, 0))
      mapper.readValue("""{"value":2,"scale":3}""", classOf[Overloaded]).value.map(_ + 1L) shouldEqual Some(7L)
    }
    // a class declared inside a method takes the enclosing instance as a hidden constructor argument,
    // which Jackson cannot supply, so it cannot be read at all - the derives neither helps nor hinders.
    // Which exception says so depends on how the compiler version shaped that constructor.
    "read a class declared inside a method as without the derives" in {
      case class Local(aLong: Option[Long]) derives ScalaTypeInfo
      case class LocalPlain(aLong: Option[Long])
      fieldsOf[Local] shouldEqual Set("aLong")
      val derived = outcome("""{"aLong":2}""", classOf[Local], _.aLong.map(_ + 1L))
      val plain = outcome("""{"aLong":2}""", classOf[LocalPlain], _.aLong.map(_ + 1L))
      derived shouldEqual plain
      derived shouldBe a[Left[?, ?]]
    }
  }
}
