package com.fasterxml.jackson.module.scala.introspect

import java.lang.reflect.Member
import com.fasterxml.jackson.module.scala.BaseSpec
import com.fasterxml.jackson.module.scala.introspect.BeanIntrospectorTest.DecodedNameMatcher
import org.junit.runner.RunWith
import org.scalatest.matchers.{HavePropertyMatchResult, HavePropertyMatcher}
import org.scalatest.{Inside, LoneElement, OptionValues}
import org.scalatestplus.junit.JUnitRunner

import scala.reflect.NameTransformer

object BeanIntrospectorTest {

  class PlainCtorBean(`field-name`: Int) {
    // This needs to exist otherwise it gets optimized away
    def x: Int = `field-name`
  }

  class ValCtorBean(val `field-name`: Int)

  case class ValCaseBean(`field-name`: Int)

  class VarCtorBean(var `field-name`: Int)

  case class VarCaseBean(var `field-name`: Int)

  class MethodBean {
    def `field-name` = 0

    def `field-name_=`(v: Int): Unit = {}
  }

  //adding @SerialVersionUID puts a public static final int field on the generated class
  //this field should be ignored
  @SerialVersionUID(8675309)
  case class SerialIDBean(field: String) {
    @transient val shouldBeExluded = 10
    @volatile var alsoExcluded = 20
  }

  class StaticMethodBean extends JavaStaticMethods {
    val included = "included"
  }

  class Parent {
    var parentValue = "parentValue"
  }

  class Child extends Parent {
    var childValue = "childValue"
  }

  case class PrivateDefaultBean(private val privateField: String = "defaultValue")

  trait DecodedNameMatcher {
    def decodedName(expectedValue: String) =
      new HavePropertyMatcher[Member, String] {
        def apply(member: Member) = {
          val decodedName = NameTransformer.decode(member.getName)
          HavePropertyMatchResult(
            decodedName == expectedValue,
            "name",
            expectedValue,
            decodedName
          )
        }
      }
  }

  class OverloadedGetter {
    var firstName: String = ""
    var lastName: String = ""

    def firstName(firstName: String): Unit = {}
    def lastName(lastName: String): Unit = {}
  }
}

@RunWith(classOf[JUnitRunner])
class BeanIntrospectorTest extends BaseSpec with Inside with LoneElement with OptionValues with DecodedNameMatcher {
  import BeanIntrospectorTest._

  behavior of "BeanIntrospector"

  it should "recognize a private [this] val field" in {

    class Bean {
      private [this] val `field-name` = 0
    }

    val beanDesc = BeanIntrospector[Bean](classOf[Bean])
    val props = beanDesc.properties

    inside (props.loneElement) { case PropertyDescriptor(n,p,f,g,s,_,_) =>
      n shouldBe "field-name"
      p shouldBe empty
      f.value should have (decodedName ("field-name"))
      g shouldBe empty
      s shouldBe empty
    }
  }

  it should "recognize a val field" in {

    class Bean {
      private val `field-name` = 0
    }

    val beanDesc = BeanIntrospector[Bean](classOf[Bean])
    val props = beanDesc.properties

    inside (props.loneElement) { case PropertyDescriptor(n,p,f,g,s,_,_) =>
      n shouldBe "field-name"
      p shouldBe empty
      f.value should have (decodedName ("field-name"))
      g.value should have (decodedName ("field-name"))
      s shouldBe empty
    }
  }

  it should "recognize a var field" in {

    class Bean {
      private var `field-name` = 0
    }

    val beanDesc = BeanIntrospector[Bean](classOf[Bean])
    val props = beanDesc.properties

    inside (props.loneElement) { case PropertyDescriptor(n,p,f,g,s,_,_) =>
      n shouldBe "field-name"
      p shouldBe empty
      f.value should have (decodedName ("field-name"))
      g.value should have (decodedName ("field-name"))
      s.value should have (decodedName ("field-name_="))
    }
  }

  it should "recognize a method property" in {

    class Bean {
      private def `field-name` = 0
      private def `field-name_=`(int: Int): Unit = {}
    }

    val beanDesc = BeanIntrospector[Bean](classOf[Bean])
    val props = beanDesc.properties

    inside (props.loneElement) { case PropertyDescriptor(n,p,f,g,s,_,_) =>
      n shouldBe "field-name"
      p shouldBe empty
      f shouldBe empty
      g.value should have (decodedName ("field-name"))
      s.value should have (decodedName ("field-name_="))
    }
  }

  it should "recognize a private constructor parameter" in {
    type Bean = PlainCtorBean

    val beanDesc = BeanIntrospector[Bean](classOf[Bean])
    val props = beanDesc.properties

    inside (props.loneElement) { case PropertyDescriptor(n,p,f,g,s,_,_) =>
      n shouldBe "field-name"
      p.value should have (Symbol("index") (0))
      f.value should have (decodedName ("field-name"))
      g shouldBe empty
      s shouldBe empty
    }
  }

  it should "recognize a val constructor parameter" in {
    type Bean = ValCtorBean

    val beanDesc = BeanIntrospector[Bean](classOf[Bean])
    val props = beanDesc.properties

    inside (props.loneElement) { case PropertyDescriptor(n,p,f,g,s,_,_) =>
      n shouldBe "field-name"
      p.value should have (Symbol("index") (0))
      f.value should have (decodedName ("field-name"))
      g.value should have (decodedName ("field-name"))
      s shouldBe empty
    }
  }

  it should "recognize a val case class parameter" in {
    type Bean = ValCaseBean

    val beanDesc = BeanIntrospector[Bean](classOf[Bean])
    val props = beanDesc.properties

    inside (props.loneElement) { case PropertyDescriptor(n,p,f,g,s,_,_) =>
      n shouldBe "field-name"
      p.value should have (Symbol("index") (0))
      f.value should have (decodedName ("field-name"))
      g.value should have (decodedName ("field-name"))
      s shouldBe empty
    }
  }

  it should "recognize a var constructor parameter" in {
    type Bean = VarCtorBean

    val beanDesc = BeanIntrospector[Bean](classOf[Bean])
    val props = beanDesc.properties

    inside (props.loneElement) { case PropertyDescriptor(n,p,f,g,s,_,_) =>
      n shouldBe "field-name"
      p.value should have (Symbol("index") (0))
      f.value should have (decodedName ("field-name"))
      g.value should have (decodedName ("field-name"))
      s.value should have (decodedName ("field-name_="))
    }
  }

  it should "recognize a var case class parameter" in {
    type Bean = VarCaseBean

    val beanDesc = BeanIntrospector[Bean](classOf[Bean])
    val props = beanDesc.properties

    inside (props.loneElement) { case PropertyDescriptor(n,p,f,g,s,_,_) =>
      n shouldBe "field-name"
      p.value should have (Symbol("index") (0))
      f.value should have (decodedName ("field-name"))
      g.value should have (decodedName ("field-name"))
      s.value should have (decodedName ("field-name_="))
    }
  }

  it should "recognize a method-only property" in {
    type Bean = MethodBean

    val beanDesc = BeanIntrospector[Bean](classOf[Bean])
    val props = beanDesc.properties

    inside (props.loneElement) { case PropertyDescriptor(n,p,f,g,s,_,_) =>
      n shouldBe "field-name"
      p shouldBe empty
      f shouldBe empty
      g.value should have (decodedName ("field-name"))
      s.value should have (decodedName ("field-name_="))
    }
  }

  it should "ignore static, synthetic, volatile and transient fields" in {
    type Bean = SerialIDBean

    val beanDesc = BeanIntrospector[Bean](classOf[Bean])
    val props = beanDesc.properties

    inside (props.loneElement) { case PropertyDescriptor(n,p,f,g,s,_,_) =>
      n shouldBe "field"
      p shouldBe defined
      f.value should have (Symbol("name") ("field"))
      g.value should have (decodedName("field"))
      s shouldBe empty
    }
  }

  it should "ignore static and synthetic methods" in {
    type Bean = StaticMethodBean

    val beanDesc = BeanIntrospector[Bean](classOf[Bean])
    val props = beanDesc.properties

    inside (props.loneElement) { case PropertyDescriptor(n, p, f, g, s, _, _) =>
      n shouldBe "included"
      p shouldBe empty
      f.value should have (Symbol("name") ("included"))
      g.value should have (decodedName ("included"))
      s shouldBe empty
    }
  }

  it should "properly introspect properties from a class hierarchy" in {
    type Bean = Child

    val beanDesc = BeanIntrospector[Bean](classOf[Bean])
    val props = beanDesc.properties

    props should have size 2
    props.map(_.name) shouldBe List("childValue", "parentValue")
  }

  it should "correctly name a case class private field in the presence of constructor defaults" in {
    type Bean = PrivateDefaultBean

    val beanDesc = BeanIntrospector[Bean](classOf[Bean])
    val props = beanDesc.properties

    props should have size 1
    props.head.name shouldBe "privateField"
  }

  it should "find getters among overloads" in {
    type Bean = OverloadedGetter

    val beanDesc = BeanIntrospector[Bean](classOf[Bean])
    val props = beanDesc.properties

    props should have size 2
    props.forall ( _.getter.isDefined ) shouldBe true
  }

  it should "handle case class with Seq member" in {
    case class ModelWSeqString(strings: Seq[String])

    val beanDesc = BeanIntrospector(classOf[ModelWSeqString])
    val props = beanDesc.properties

    props should have size 1
    props.head.name shouldBe "strings"
    //scala 2.13 has scala.collection.immutable.Seq instead of scala.collection.Seq
    props.head.field.value.getAnnotatedType.getType.getTypeName should endWith ("Seq<java.lang.String>")
  }
}
