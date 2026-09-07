package tools.jackson.module.scala.util

import tools.jackson.module.scala.{BaseSpec, Weekday}

import java.io.ByteArrayOutputStream
import java.net.{URI, URL}

/**
 * Loads a class itself rather than delegating, and answers for the .tasty file of anything it
 * loaded - which stands in for the container, OSGi or script engine loader that holds an
 * application's classes while this module is held by another.
 */
private class TastyChildLoader(parent: ClassLoader) extends ClassLoader(parent) {

  var asked: List[String] = Nil

  def define(name: String): Class[_] = {
    val stream = parent.getResourceAsStream(name.replace('.', '/') + ".class")
    val bytes = new ByteArrayOutputStream()
    try {
      val chunk = new Array[Byte](8192)
      var read = stream.read(chunk)
      while (read >= 0) {
        bytes.write(chunk, 0, read)
        read = stream.read(chunk)
      }
    } finally stream.close()
    val defined = bytes.toByteArray
    defineClass(name, defined, 0, defined.length)
  }

  override def getResource(name: String): URL = {
    asked = name :: asked
    if (name.endsWith(".tasty")) URI.create(s"file:///$name").toURL else super.getResource(name)
  }
}

class TastyUtilTest extends BaseSpec {

  "TastyUtil.hasTastyFile" should "support EnumResolver (class)" in {
    TastyUtil.hasTastyFile(classOf[EnumResolver]) shouldBe true
  }
  it should "support TastyUtil (object)" in {
    TastyUtil.hasTastyFile(TastyUtil.getClass) shouldBe true
  }
  it should "support Weekday (scala2 enum)" in {
    TastyUtil.hasTastyFile(Weekday.getClass) shouldBe true
  }
  it should "support ColorEnum (scala3 enum)" in {
    TastyUtil.hasTastyFile(ColorEnum.getClass) shouldBe true
  }
  it should "support JavaCompatibleColorEnum (scala3 enum)" in {
    TastyUtil.hasTastyFile(JavaCompatibleColorEnum.getClass) shouldBe true
  }
  it should "support EnclosingObject.EnclosedColorEnum (scala3 enum)" in {
    TastyUtil.hasTastyFile(EnclosingObject.EnclosedColorEnum.getClass) shouldBe true
  }
  it should "support EnclosingObject.EnclosedScala3Class (scala3 enum)" in {
    TastyUtil.hasTastyFile(classOf[EnclosingObject.EnclosedScala3Class]) shouldBe true
  }
  it should "not support Java class" in {
    TastyUtil.hasTastyFile(classOf[String]) shouldBe false
  }
  // TastyProbe is a Java class, so the classpath holds no .tasty for it. The only one that exists is
  // the one its loader makes up, and that loader is reached only by asking the class itself.
  it should "ask the classloader of the class it was handed" in {
    val loader = new TastyChildLoader(getClass.getClassLoader)
    val loaded = loader.define(classOf[TastyProbe].getName)
    loaded.getClassLoader shouldBe loader
    TastyUtil.hasTastyFile(loaded) shouldBe true
    loader.asked should contain ("tools/jackson/module/scala/util/TastyProbe.tasty")
  }
}
