package tools.jackson.module.scala

import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

case class Erased(aLong: Option[Long], anInt: Option[Int], aStr: Option[String],
                  longs: Seq[Long], plain: String) derives ScalaTypeInfo

class ScalaTypeInfoSpec extends AnyWordSpec with Matchers with BeforeAndAfterEach {

  private val json = """{"aLong":2,"anInt":1,"aStr":"x","longs":[3],"plain":"p"}"""

  override def afterEach(): Unit = ScalaAnnotationIntrospectorModule.clearRegisteredReferencedTypes()

  private def mapper = JsonMapper.builder().addModule(DefaultScalaModule).build()

  private def registerFromDerivedTable(): Unit = {
    val companion = Class.forName(classOf[Erased].getName + "$")
    val instance = companion.getField("MODULE$").get(None.orNull)
    val table = companion.getMethod("derived$ScalaTypeInfo").invoke(instance).asInstanceOf[ScalaTypeInfo[_]]
    table.erasedTypeArguments.foreach { case (field, clazz) =>
      ScalaAnnotationIntrospectorModule.registerReferencedValueType(classOf[Erased], field, clazz)
    }
  }

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
    // Option[Int] happens to work: a small JSON number is read as an Integer, which is what Int
    // boxes to. Option[Long] is where erasure bites.
    "describe what erasure breaks today" in {
      val read = mapper.readValue(json, classOf[Erased])
      read.anInt.map(_.getClass.getName) shouldEqual Some("int")
      intercept[ClassCastException] {
        read.aLong.map(_ + 1L)
      }
      intercept[ClassCastException] {
        read.longs.map(_ + 1L)
      }
    }
    "fix it once the captured types are registered" in {
      registerFromDerivedTable()
      val read = mapper.readValue(json, classOf[Erased])
      read.aLong.map(_.getClass.getName) shouldEqual Some("long")
      read.aLong.map(_ + 1L) shouldEqual Some(3L)
      read.longs.map(_ + 1L) shouldEqual Seq(4L)
    }
  }
}
