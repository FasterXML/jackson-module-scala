package tools.jackson.module.scala

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

class ScalaTypeInfoSpec extends AnyWordSpec with Matchers with BeforeAndAfterEach {

  private val json = """{"aLong":2,"anInt":1,"aStr":"x","longs":[3],"plain":"p"}"""

  override def afterEach(): Unit = ScalaAnnotationIntrospectorModule.clearRegisteredReferencedTypes()

  private def mapper = JsonMapper.builder().addModule(DefaultScalaModule).build()

  "ScalaTypeInfo" should {
    "capture only the type arguments the JVM erases" in {
      val captured = summon[ScalaTypeInfo[Erased]].erasedTypeArguments.toMap
      captured.keySet shouldEqual Set("aLong", "anInt", "longs")
      captured("aLong") shouldEqual classOf[Long]
      captured("longs") shouldEqual classOf[Long]
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
    "reach a type argument however deeply it is nested" in {
      val captured = summon[ScalaTypeInfo[Layered]].erasedTypeArguments.toMap
      captured("twice") shouldEqual classOf[Long]
      captured("inSeq") shouldEqual classOf[Long]
      captured("ofOption") shouldEqual classOf[Long]
      captured("mapValue") shouldEqual classOf[Long]
      // the primitive is the key here, and the module replaces the value type - naming it would
      // turn a field that merely loses its key type into one that cannot be read at all
      captured.keySet should not contain "mapKey"
    }
    "read every nested shape back with nothing registered by hand" in {
      val json = """{"twice":2,"inSeq":[2],"ofOption":[2],"mapValue":{"a":2},"mapKey":{"2":"a"}}"""
      val read = mapper.readValue(json, classOf[Layered])
      read.twice.map(_.map(_ + 1L)) shouldEqual Some(Some(3L))
      read.inSeq.map(_.map(_ + 1L)) shouldEqual Some(Seq(3L))
      read.ofOption.map(_.map(_ + 1L)) shouldEqual Seq(Some(3L))
      read.mapValue.map { case (_, v) => v + 1L } shouldEqual Seq(3L)
    }
    "leave a registration made by hand alone" in {
      // registered before anything introspects the class, so deriving would be the one to clobber it
      ScalaAnnotationIntrospectorModule
        .registerReferencedValueType(classOf[ErasedPrecedence], "aLong", classOf[Int])
      mapper.readValue("""{"aLong":2}""", classOf[ErasedPrecedence])
      ScalaAnnotationIntrospectorModule
        .getRegisteredReferencedValueType(classOf[ErasedPrecedence], "aLong") shouldEqual Some(classOf[Int])
    }
  }
}
