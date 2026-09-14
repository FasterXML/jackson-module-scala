package tools.jackson.module.scala.deser

import com.fasterxml.jackson.annotation.JsonTypeInfo
import tools.jackson.databind.DefaultTyping
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import tools.jackson.module.scala.DefaultScalaModule
import tools.jackson.module.scala.`enum`.{ColorEnum, JavaCompatibleColorEnum, ResultEnum, ShapeEnumAnnotated}
import tools.jackson.module.scala.`enum`.adt.Color
import tools.jackson.module.scala.poly.{Beast, Phantom, Raven, Status, Wolf}

object Scala3DefaultTypingRoundtripTest {
  case class HasEnum(c: ColorEnum)
  case class HasOptionEnum(c: Option[ColorEnum])
  case class HasEnumList(l: List[ColorEnum])
  case class HasEnumKeyMap(m: Map[ColorEnum, Int])
  case class HasEnumValueMap(m: Map[String, ColorEnum])
  case class HasJavaEnum(c: JavaCompatibleColorEnum)
  case class HasAdt(r: ResultEnum)
  case class HasAdtList(l: List[ResultEnum])
  case class HasOptionAdt(o: Option[ResultEnum])
  case class HasParameterizedAdtSet(s: Set[Color])
  case class HasMarkedEnum(s: Status)
  case class HasDerived(b: Beast)
  case class HasDerivedList(l: List[Beast])
  case class HasAnnotatedEnum(s: ShapeEnumAnnotated)
  case class HasIArray(a: IArray[Int])
  case class HasAny(a: Any)
}

// Every Scala 3 type the module serializes itself, written and read back with default typing
// enabled, over both applicability modes and every inclusion shape
class Scala3DefaultTypingRoundtripTest extends DeserializerTest {
  import Scala3DefaultTypingRoundtripTest._

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

  "Scala Module" should "name the enum in a simple case's type id, not the anonymous class of its cases" in {
    val mapper = mapperWith(DefaultTyping.OBJECT_AND_NON_CONCRETE, JsonTypeInfo.As.WRAPPER_ARRAY)
    mapper.writeValueAsString(HasEnum(ColorEnum.Green)) shouldEqual
      """{"c":["tools.jackson.module.scala.enum.ColorEnum","Green"]}"""
    mapper.writeValueAsString(HasAdt(ResultEnum.Pending)) shouldEqual
      """{"r":["tools.jackson.module.scala.enum.ResultEnum","Pending"]}"""
  }

  it should "type a parameterized case by its own class, without also tagging it" in {
    val mapper = mapperWith(DefaultTyping.OBJECT_AND_NON_CONCRETE, JsonTypeInfo.As.PROPERTY)
    mapper.writeValueAsString(HasAdt(ResultEnum.Ok("v"))) shouldEqual
      """{"r":{"@class":"tools.jackson.module.scala.enum.ResultEnum$Ok","value":"v"}}"""
    mapper.writeValueAsString(HasDerived(Wolf("w"))) shouldEqual
      """{"b":{"@class":"tools.jackson.module.scala.poly.Wolf","name":"w"}}"""
    mapper.writeValueAsString(HasDerived(Phantom)) shouldEqual
      """{"b":{"@class":"tools.jackson.module.scala.poly.Phantom$"}}"""
    // and the tag as before where no type id is in play
    newMapper.writeValueAsString(HasAdt(ResultEnum.Ok("v"))) shouldEqual """{"r":{"@type":"Ok","value":"v"}}"""
  }

  it should "write a root-level enum or marked value with the type id its root type gets, without writerFor" in {
    val mapper = mapperWith(DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY)
    mapper.writeValueAsString(ColorEnum.Red) shouldEqual """["tools.jackson.module.scala.enum.ColorEnum","Red"]"""
    mapper.writeValueAsString(ResultEnum.Ok("v")) shouldEqual
      """{"@class":"tools.jackson.module.scala.enum.ResultEnum$Ok","value":"v"}"""
    mapper.writeValueAsString(Wolf("w")) shouldEqual """{"@class":"tools.jackson.module.scala.poly.Wolf","name":"w"}"""
    newMapper.writeValueAsString(ColorEnum.Red) shouldEqual "\"Red\""
    newMapper.writeValueAsString(Wolf("w")) shouldEqual """{"@type":"Wolf","name":"w"}"""
  }

  for ((typing, as) <- shapes) {
    val mapper = mapperWith(typing, as)

    it should s"roundtrip simple enums with default typing $typing/$as" in {
      roundtrip(mapper, HasEnum(ColorEnum.Green), classOf[HasEnum])
      roundtrip(mapper, HasOptionEnum(Some(ColorEnum.Green)), classOf[HasOptionEnum])
      roundtrip(mapper, HasOptionEnum(None), classOf[HasOptionEnum])
      roundtrip(mapper, HasEnumList(List(ColorEnum.Red, ColorEnum.Blue)), classOf[HasEnumList])
      roundtrip(mapper, HasEnumKeyMap(Map(ColorEnum.Red -> 1)), classOf[HasEnumKeyMap])
      roundtrip(mapper, HasEnumValueMap(Map("a" -> ColorEnum.Red)), classOf[HasEnumValueMap])
      roundtrip(mapper, HasJavaEnum(JavaCompatibleColorEnum.Green), classOf[HasJavaEnum])
      roundtrip(mapper, HasAny(ColorEnum.Red), classOf[HasAny])
    }

    it should s"roundtrip enums with parameterized cases with default typing $typing/$as" in {
      roundtrip(mapper, HasAdt(ResultEnum.Ok("v")), classOf[HasAdt])
      roundtrip(mapper, HasAdt(ResultEnum.Error(2)), classOf[HasAdt])
      roundtrip(mapper, HasAdt(ResultEnum.Pending), classOf[HasAdt])
      roundtrip(mapper, HasAdtList(List(ResultEnum.Ok("v"), ResultEnum.Pending)), classOf[HasAdtList])
      roundtrip(mapper, HasOptionAdt(Some(ResultEnum.Error(2))), classOf[HasOptionAdt])
      roundtrip(mapper, HasParameterizedAdtSet(Set(Color.Red, Color.Mix(5))), classOf[HasParameterizedAdtSet])
      roundtrip(mapper, HasAny(ResultEnum.Ok("v")), classOf[HasAny])
      roundtrip(mapper, HasAny(ResultEnum.Pending), classOf[HasAny])
    }

    it should s"roundtrip a SealedPolymorphismSupport enum with default typing $typing/$as" in {
      roundtrip(mapper, HasMarkedEnum(Status.Active), classOf[HasMarkedEnum])
      roundtrip(mapper, HasMarkedEnum(Status.Failed(1)), classOf[HasMarkedEnum])
    }

    it should s"roundtrip a hierarchy deriving SealedSubtypes with default typing $typing/$as" in {
      roundtrip(mapper, HasDerived(Wolf("w")), classOf[HasDerived])
      roundtrip(mapper, HasDerived(Phantom), classOf[HasDerived])
      roundtrip(mapper, HasDerived(Raven(1)), classOf[HasDerived])
      roundtrip(mapper, HasDerivedList(List(Wolf("w"), Phantom, Raven(1))), classOf[HasDerivedList])
    }

    it should s"roundtrip an enum carrying its own @JsonTypeInfo and an IArray with default typing $typing/$as" in {
      roundtrip(mapper, HasAnnotatedEnum(ShapeEnumAnnotated.Circle(1.0)), classOf[HasAnnotatedEnum])
      val json = mapper.writeValueAsString(HasIArray(IArray(1, 2)))
      withClue(json) {
        mapper.readValue(json, classOf[HasIArray]).a.toSeq shouldEqual Seq(1, 2)
      }
    }

    it should s"roundtrip root-level enums and marked values without writerFor with default typing $typing/$as" in {
      mapper.readValue(mapper.writeValueAsString(ColorEnum.Red), classOf[ColorEnum]) shouldEqual ColorEnum.Red
      mapper.readValue(mapper.writeValueAsString(ResultEnum.Ok("v")), classOf[ResultEnum]) shouldEqual ResultEnum.Ok("v")
      mapper.readValue(mapper.writeValueAsString(ResultEnum.Pending), classOf[ResultEnum]) shouldEqual ResultEnum.Pending
      mapper.readValue(mapper.writeValueAsString(Status.Active), classOf[Status]) shouldEqual Status.Active
      mapper.readValue(mapper.writeValueAsString(Status.Failed(1)), classOf[Status]) shouldEqual Status.Failed(1)
      mapper.readValue(mapper.writeValueAsString(Wolf("w")), classOf[Beast]) shouldEqual Wolf("w")
      mapper.readValue(mapper.writeValueAsString(Phantom), classOf[Beast]) shouldEqual Phantom
    }
  }
}
