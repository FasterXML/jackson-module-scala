package com.fasterxml.jackson.module.scala.introspect

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import reflect.NameTransformer
import java.lang.reflect.Modifier

class PlainCtorBean(`field-name`: Int)
{
  // This needs to exist otherwise it gets optimized away
  def x = `field-name`
}

class ValCtorBean(val `field-name`: Int)
case class ValCaseBean(`field-name`: Int)

class VarCtorBean(var `field-name`: Int)
case class VarCaseBean(var `field-name`: Int)

class MethodBean
{
  def `field-name` = 0
  def `field-name_=`(v: Int) { }
}

//adding @SerialVersionUID puts a public static final int field on the generated class
//this field should be ignored
@SerialVersionUID(uid = 8675309)
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

@RunWith(classOf[JUnitRunner])
class BeanIntrospectorTest extends FlatSpec with ShouldMatchers {

  behavior of "BeanIntrospector"

  it should "recognize a private [this] val field" in {

    class Bean {
      private [this] val `field-name` = 0
    }

    val beanDesc = BeanIntrospector[Bean](classOf[Bean])
    val props = beanDesc.properties

    props should have size (1)
    props.head match {
      case PropertyDescriptor(name, None, Some(f), None, None) =>
        name should be === ("field-name")
        NameTransformer.decode(f.getName) should be === ("field-name")
      case _ => assert(false, "Property does not have the correct format")
    }

  }

  it should "recognize a val field" in {

    class Bean {
      private val `field-name` = 0
    }

    val beanDesc = BeanIntrospector[Bean](classOf[Bean])
    val props = beanDesc.properties

    props should have size (1)
    props.head match {
      case PropertyDescriptor(name, None, Some(f), Some(g), None) =>
        name should be === ("field-name")
        NameTransformer.decode(f.getName) should be === ("field-name")
        NameTransformer.decode(g.getName) should be === ("field-name")
      case _ => assert(false, "Property does not have the correct format")
    }

  }

  it should "recognize a var field" in {

    class Bean {
      private var `field-name` = 0
    }

    val beanDesc = BeanIntrospector[Bean](classOf[Bean])
    val props = beanDesc.properties

    props should have size (1)
    props.head match {
      case PropertyDescriptor(name, None, Some(f), Some(g), Some(s)) =>
        name should be === ("field-name")
        NameTransformer.decode(f.getName) should be === ("field-name")
        NameTransformer.decode(g.getName) should be === ("field-name")
        NameTransformer.decode(s.getName) should be === ("field-name_=")
      case _ => assert(false, "Property does not have the correct format")
    }

  }

  it should "recognize a method property" in {

    class Bean {
      private def `field-name` = 0
      private def `field-name_=`(int: Int) {}
    }

    val beanDesc = BeanIntrospector[Bean](classOf[Bean])
    val props = beanDesc.properties

    props should have size (1)
    props.head match {
      case PropertyDescriptor(name, None, None, Some(g), Some(s)) =>
        name should be === ("field-name")
        NameTransformer.decode(g.getName) should be === ("field-name")
        NameTransformer.decode(s.getName) should be === ("field-name_=")
      case _ => assert(false, "Property does not have the correct format")
    }

  }

  it should "recognize a private constructor parameter" in {
    type Bean = PlainCtorBean

    val beanDesc = BeanIntrospector[Bean](classOf[Bean])
    val props = beanDesc.properties

    props should have size (1)
    props.head match {
      case PropertyDescriptor(name, Some(p), Some(f), None, None) =>
        name should be === ("field-name")
        p.index should be === (0)
        NameTransformer.decode(f.getName) should be === ("field-name")
      case _ => assert(false, "Property does not have the correct format")
    }

  }

  it should "recognize a val constructor parameter" in {
    type Bean = ValCtorBean

    val beanDesc = BeanIntrospector[Bean](classOf[Bean])
    val props = beanDesc.properties

    props should have size (1)
    props.head match {
      case PropertyDescriptor(name, Some(p), Some(f), Some(g), None) =>
        name should be === ("field-name")
        p.index should be === (0)
        NameTransformer.decode(f.getName) should be === ("field-name")
        NameTransformer.decode(g.getName) should be === ("field-name")
      case _ => assert(false, "Property does not have the correct format")
    }

  }

  it should "recognize a val case class parameter" in {
    type Bean = ValCaseBean

    val beanDesc = BeanIntrospector[Bean](classOf[Bean])
    val props = beanDesc.properties

    props should have size (1)
    props.head match {
      case PropertyDescriptor(name, Some(p), Some(f), Some(g), None) =>
        name should be === ("field-name")
        p.index should be === (0)
        NameTransformer.decode(f.getName) should be === ("field-name")
        NameTransformer.decode(g.getName) should be === ("field-name")
      case _ => assert(false, "Property does not have the correct format")
    }

  }

  it should "recognize a var constructor parameter" in {
    type Bean = VarCtorBean

    val beanDesc = BeanIntrospector[Bean](classOf[Bean])
    val props = beanDesc.properties

    props should have size (1)
    props.head match {
      case PropertyDescriptor(name, Some(p), Some(f), Some(g), Some(s)) =>
        name should be === ("field-name")
        p.index should be === (0)
        NameTransformer.decode(f.getName) should be === ("field-name")
        NameTransformer.decode(g.getName) should be === ("field-name")
        NameTransformer.decode(s.getName) should be === ("field-name_=")
      case _ => assert(false, "Property does not have the correct format")
    }

  }

  it should "recognize a var case class parameter" in {
    type Bean = VarCaseBean

    val beanDesc = BeanIntrospector[Bean](classOf[Bean])
    val props = beanDesc.properties

    props should have size (1)
    props.head match {
      case PropertyDescriptor(name, Some(p), Some(f), Some(g), Some(s)) =>
        name should be === ("field-name")
        p.index should be === (0)
        NameTransformer.decode(f.getName) should be === ("field-name")
        NameTransformer.decode(g.getName) should be === ("field-name")
        NameTransformer.decode(s.getName) should be === ("field-name_=")
      case _ => assert(false, "Property does not have the correct format")
    }

  }

  it should "recognize a method-only property" in {
    type Bean = MethodBean

    val beanDesc = BeanIntrospector[Bean](classOf[Bean])
    val props = beanDesc.properties

    props should have size (1)
    props.head match {
      case PropertyDescriptor(name, None, None, Some(g), Some(s)) =>
        name should be === ("field-name")
        NameTransformer.decode(g.getName) should be === ("field-name")
        NameTransformer.decode(s.getName) should be === ("field-name_=")
      case _ => assert(false, "Property does not have the correct format")
    }

  }

  it should "ignore static, synthetic, volatile and transient fields" in {
    type Bean = SerialIDBean

    val beanDesc = BeanIntrospector[Bean](classOf[Bean])
    val props = beanDesc.properties

    props should have size (1)
    props.head match {
      case PropertyDescriptor(name, Some(cp), Some(f), Some(g), None) =>
        name should be === ("field")
        f.getName should be === ("field")
        NameTransformer.decode(g.getName) should be === ("field")
      case _ => assert(false, "Property does not have the correct format")
    }
  }

  it should "ignore static and synthetic methods" in {
    type Bean = StaticMethodBean

    val beanDesc = BeanIntrospector[Bean](classOf[Bean])
    val props = beanDesc.properties

    props should have size (1)
    props.head match {
      case PropertyDescriptor(name, None, Some(f), Some(g), None) =>
        name should be === ("included")
        f.getName should be === ("included")
        NameTransformer.decode(g.getName) should be === ("included")
      case _ => assert(false, "Property does not have the correct format")
    }
  }

  it should "properly introspect properties from a class hierarchy" in {
    type Bean = Child

    val beanDesc = BeanIntrospector[Bean](classOf[Bean])
    val props = beanDesc.properties

    props should have size (2)
    props.map(_.name) should be === List("parentValue", "childValue")
  }

  it should "correctly name a case class private field in the presence of constructor defaults" in {
    type Bean = PrivateDefaultBean

    val beanDesc = BeanIntrospector[Bean](classOf[Bean])
    val props = beanDesc.properties

    props should have size (1)
    props.head match {
      case PropertyDescriptor(name, _, _, _, _) =>
        name should be === ("privateField")
      case _ => assert(false, "Property does not have the correct format")
    }
  }
}


