package com.fasterxml.jackson.module.scala.reflect

import scala.reflect.runtime.universe._

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import com.fasterxml.jackson.databind.introspect.{AnnotatedMethod, AnnotationMap, AnnotatedConstructor}

case class MethodSignatureTestClass(number: Int, text: String) {
  def setGenericVal[T >: AnyVal](value: T) { /* ignore */ }
  def getValue: String = ""
}

@RunWith(classOf[JUnitRunner])
class MethodSignatureTest extends FlatSpec with ShouldMatchers {
  "A MethodSignature created from a MethodSymbol" should "equal one created from an AnnotatedConstructor" in {
    val constructor = typeOf[MethodSignatureTestClass].members.filter(symbol => symbol.isMethod && symbol.asMethod.isConstructor).toArray.apply(0).asMethod
    val jConstructor = new AnnotatedConstructor(classOf[MethodSignatureTestClass].getConstructors.apply(0), new AnnotationMap(), Array())
    MethodSignature(constructor) should be (MethodSignature(jConstructor))
  }

  "A MethodSignature with generic parameter created from a MethodSymbol" should "equal one created from an AnnotatedMethod" in {
    val method = typeOf[MethodSignatureTestClass].member(newTermName("setGenericVal")).asMethod
    val jMethod = new AnnotatedMethod(classOf[MethodSignatureTestClass].getMethod("setGenericVal", classOf[Object]), new AnnotationMap(), Array())
    MethodSignature(method) should be (MethodSignature(jMethod))
  }

  "An empty MethodSignature" should "match a relevant method" in {
    val method = typeOf[MethodSignatureTestClass].member(newTermName("getValue")).asMethod
    MethodSignature() should be (MethodSignature(method))
  }
}
