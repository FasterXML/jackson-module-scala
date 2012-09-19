package com.fasterxml.jackson.module.scala.reflect

import reflect.runtime.universe._

sealed case class BeanProperty(name: String, typ: Type, readable: Boolean, writable: Boolean,
                               constructor: Option[MethodSignature] = None, constructorParameterIndex: Option[Int] = None)

object BeanProperty {
  def getter(name: String, typ: Type) = apply(name, typ, readable = true, writable = false)
  def setter(name: String, typ: Type) = apply(name, typ, readable = false, writable = true)
  def constructor(name: String, typ: Type, constructor: MethodSignature, constructorParameterIndex: Int) =
    apply(name, typ, readable = false, writable = true,
      constructor = Some(constructor), constructorParameterIndex = Some(constructorParameterIndex))
  def merge(a: BeanProperty, b: BeanProperty) = apply(a.name, a.typ,
    a.readable || b.readable, a.writable || b.writable,
    if (a.constructor.isDefined) a.constructor else b.constructor,
    if (a.constructorParameterIndex.isDefined) a.constructorParameterIndex else b.constructorParameterIndex)
}
