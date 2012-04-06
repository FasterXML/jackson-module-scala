package com.fasterxml.jackson.module.scala.util

import org.scalastuff.scalabeans.types.ScalaType
import org.scalastuff.scalabeans.sig.{Mirror, ClassDeclExtractor}
import org.scalastuff.scalabeans.PropertyDescriptor
import org.scalastuff.scalabeans.Preamble._

private abstract class ScalaTypeImpl(val erasure: Class[_], val arguments: ScalaType*) extends ScalaType

object ScalaBeansUtil {
  def propertiesOf(cls: Class[_]): Seq[PropertyDescriptor] = {
    val anyType = scalaTypeOf(classOf[Any])
    val scalaType = scalaTypeOf(cls)

    val typeWithFakeParamTypes = for {
      top <- ClassDeclExtractor.extract(scalaType.erasure)
      classDecl <- top.headOption
      if classDecl.isInstanceOf[Mirror.ClassDecl]
    } yield {
      val args = classDecl.asInstanceOf[Mirror.ClassDecl].typeParameters.map(_ => anyType)
      new ScalaTypeImpl(cls, args: _*) {}
    }

    val typ = typeWithFakeParamTypes.getOrElse(scalaType)
    descriptorOf(typ).properties
  }
}
