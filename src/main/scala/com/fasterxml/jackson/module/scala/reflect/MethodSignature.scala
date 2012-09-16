package com.fasterxml.jackson.module.scala.reflect

import reflect.runtime.currentMirror
import scala.reflect.runtime.universe._
import com.fasterxml.jackson.databind.introspect.AnnotatedWithParams


class MethodSignature private(val parameterTypes: List[Symbol]) {
  override def equals(obj: Any) = obj match {
    case other: MethodSignature => parameterTypes == other.parameterTypes
    case _ => false
  }

  override def hashCode() = parameterTypes.hashCode()

  override def toString = s"MethodSignature($parameterTypes)"
}

object MethodSignature {
  private lazy val emptySignature = new MethodSignature(Nil)

  def apply() = emptySignature

  def apply(params: List[Type]) = new MethodSignature(params.map(_.erasure.typeSymbol))

  private def getParameterType(param: Symbol) = param.typeSignature.erasure.typeSymbol

  def apply(method: MethodSymbol): MethodSignature = method.params match {
    case List(params) => new MethodSignature(params.map(getParameterType(_)))
    case _ => new MethodSignature(Nil)
  }

  private def getParameterType(param: Class[_]) = currentMirror.classSymbol(param)

  def apply(param: Class[_]): MethodSignature = new MethodSignature(List(getParameterType(param)))

  private def buildParameterList(params: List[Symbol], method: AnnotatedWithParams, index: Int): List[Symbol] = {
    if (index < 0)
      params
    else
      buildParameterList(getParameterType(method.getRawParameterType(index)) :: params, method, index - 1)
  }

  def apply(method: AnnotatedWithParams) = new MethodSignature(buildParameterList(Nil, method, method.getParameterCount - 1))
}