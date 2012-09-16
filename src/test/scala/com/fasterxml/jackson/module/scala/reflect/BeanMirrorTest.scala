package com.fasterxml.jackson.module.scala.reflect

import reflect.runtime.universe._

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

final class NonBean(val number: Int, var text: String)

sealed case class TestBean(number: Int, var text: String, private var secret: String) {
  def noProperty(key: String): String = ""
  def dynamicProperty = ""
  var answer = 42
  val question = "universe"
}

@RunWith(classOf[JUnitRunner])
class BeanMirrorTest extends FlatSpec with ShouldMatchers {
  val beanMirror = BeanMirror(classOf[TestBean])

  "A BeanMirror" should "not be creatable for non-case classes" in {
    intercept[IllegalArgumentException] {
      BeanMirror(classOf[NonBean])
    }
  }

  it should "recognize readable properties" in {
    beanMirror.readableProperties.keySet shouldBe Set("number", "text", "dynamicProperty", "answer", "question")
  }

  it should "recognize writable properties" in {
    beanMirror.writableProperties.keySet shouldBe Set("number", "text", "secret", "answer")
  }

  it should "recognize the full list of properties" in {
    beanMirror.properties.keySet shouldBe Set("number", "text", "secret", "dynamicProperty", "answer", "question")
  }

  it should "recognize constructor vals as readable and constructor writable" in {
    beanMirror.hasGetter("number", classOf[Int]) should be(true)
    beanMirror.hasSetter("number", classOf[Int]) should be(true)
    val prop = beanMirror.properties("number")
    prop.name shouldBe "number"
    prop.typ shouldBe typeOf[Int]
    prop.writable should be(true)
    prop.readable should be(true)
    prop.constructor shouldBe Some(MethodSignature(List(typeOf[Int], typeOf[String], typeOf[String])))
    prop.constructorParameterIndex should be(Some(0))
  }

  it should "recognize private constructor vars as constructor writable only" in {
    beanMirror.readableProperties.get("secret") should  be(None)
    beanMirror.hasSetter("secret", classOf[String]) should be(true)
    val prop = beanMirror.properties("secret")
    prop.name shouldBe "secret"
    prop.typ shouldBe typeOf[String]
    prop.writable should be(true)
    prop.readable should be(false)
    prop.constructor shouldBe Some(MethodSignature(List(typeOf[Int], typeOf[String], typeOf[String])))
    prop.constructorParameterIndex should be(Some(2))
  }

  it should "recognize non-constructor vals as readable only" in {
    val prop = beanMirror.properties("question")
    prop.name should be("question")
    prop.typ.erasure.typeSymbol should be(typeOf[String].erasure.typeSymbol)
    prop.readable should be(true)
    prop.writable should be(false)
    prop.constructor should be(None)
    prop.constructorParameterIndex should be(None)
  }

  it should "recognize getter methods as readable only" in {
    val prop = beanMirror.properties("dynamicProperty")
    prop.name should be("dynamicProperty")
    prop.typ.erasure.typeSymbol should be(typeOf[String].erasure.typeSymbol)
    prop.readable should be(true)
    prop.writable should be(false)
    prop.constructor should be(None)
    prop.constructorParameterIndex should be(None)
  }

  it should "recognize vars as readable and writable" in {
    beanMirror.properties("answer") should be(BeanProperty("answer", typeOf[Int], true, true, None, None))
  }

  it should "have a constructor parameter named 'secret' at index 2" in {
    val signature = MethodSignature(List(typeOf[Int], typeOf[String], typeOf[String]))
    beanMirror.getConstructorParameterName(signature, 2) should be(Some("secret"))
  }
}
