package com.fasterxml.jackson.module.scala.reflect

import reflect.runtime.currentMirror
import reflect.runtime.universe._
import scala.Some


class BeanMirror private (private val symbol: ClassSymbol) {
  private val typ = symbol.toType
  lazy val properties = findProperties
  lazy val readableProperties = properties.filter(_._2.readable).toMap
  lazy val writableProperties = properties.filter(_._2.writable).toMap

  private def findMethod(signature: MethodSignature, candidates: Iterable[Symbol]) = {
    candidates.find(_ match {
      case method: MethodSymbol if method.isPublic => signature == MethodSignature(method)
      case _ => false
    }) match {
      case Some(symbol: MethodSymbol) => Some(symbol)
      case _ => None
    }
  }

  def getConstructorParameterName(signature: MethodSignature, paramIndex: Int): Option[String] = {
    require(paramIndex >= 0 && paramIndex < signature.parameterTypes.length)

    val constructors = typ.members.collect {
      case symbol: MethodSymbol if symbol.isConstructor => symbol
    }
    findMethod(signature, constructors) match {
      case Some(constructor: MethodSymbol) => Some(constructor.params.head.apply(paramIndex).name.decoded)
      case None => None
    }
  }

  private def hasProperty(props: Map[String, BeanProperty], propertyName: String, propertyType: Class[_]) = props.get(propertyName) match {
    case Some(property) => property.typ.erasure.typeSymbol == currentMirror.classSymbol(propertyType).typeSignature.typeSymbol
    case None => false
  }

  def hasGetter(propertyName: String, propertyType: Class[_]) = hasProperty(readableProperties, propertyName, propertyType)

  def hasSetter(propertyName: String, propertyType: Class[_]) = hasProperty(writableProperties, propertyName, propertyType)

  private def mergeProperties(a: Map[String, BeanProperty], b: Map[String, BeanProperty]) = {
    (a.keySet ++ b.keySet).map(name => (a.get(name), b.get(name)) match {
      case (Some(a), Some(b)) => BeanProperty.merge(a, b)
      case (Some(a), None) => a
      case (None, Some(b)) => b
    }).map(property => (property.name, property)).toMap
  }

  private def findReadableProperties(beanType: Type): Map[String, BeanProperty] = beanType.members.collect {
    case method: MethodSymbol if method.isPublic && !method.isSynthetic && MethodSignature(method) == MethodSignature() && method.returnType != typeOf[Unit] => method
  }.map(method => (method.name.decoded, BeanProperty.getter(method.name.decoded, method.returnType))).toMap

  private def findReadableProperties: Map[String, BeanProperty] = {
    val baseProperties = findReadableProperties(typeOf[BasicCaseClass])
    val typeProperties = findReadableProperties(typ)

    typeProperties -- baseProperties.keys
  }

  private def findWritableProperties(beanType: Type): Map[String, BeanProperty] = {
    beanType.members.map(_ match {
      case method: MethodSymbol
        if !method.isSynthetic && method.isPublic &&
          method.returnType.erasure == typeOf[Unit].erasure => method.name.decoded match {
        case BeanMirror.SetterMethodNamePattern(name) => method.params match {
          case List(List(param)) => Some(BeanProperty.setter(name, param.typeSignature))
          case _ => None
        }
        case _ => None
      }
      case _ => None
    }).collect {
      case Some(property) => (property.name, property)
    }.toMap
  }

  private def findConstructorProperties = {
    def getPropertiesFromParameterList(constructor: MethodSignature,
                                       parameterIndex: Int,
                                       params: List[Symbol]): List[BeanProperty] = params match {
      case param :: tail =>
        BeanProperty.constructor(param.name.decoded, param.typeSignature, constructor, parameterIndex) ::
          getPropertiesFromParameterList(constructor, parameterIndex + 1, tail)
      case Nil => Nil
    }

    val constructors = typ.members.collect {
      case method: MethodSymbol
        if method.isConstructor && method.isPublic && !method.params.isEmpty && !method.params.head.isEmpty => method
    }
    constructors.map(constructor => constructor.params.head match {
      case parameters: List[Symbol] if !parameters.isEmpty =>
        getPropertiesFromParameterList(MethodSignature(constructor), 0, parameters)
    }).flatten.map(property => (property.name, property)).toMap
  }

  private def findWritableProperties: Map[String, BeanProperty] = {
    val baseProperties = findWritableProperties(typeOf[BasicCaseClass])
    val typeProperties = findWritableProperties(typ)

    val setters = typeProperties -- baseProperties.keySet
    val constructorProperties = findConstructorProperties
    mergeProperties(setters, constructorProperties)
  }

  private def findProperties: Map[String, BeanProperty] = {
    mergeProperties(findReadableProperties, findWritableProperties)
  }

  private sealed case class BasicCaseClass()
}

object BeanMirror {
  private val SetterMethodNamePattern = "(.*)_=".r

  def apply(clazz: Class[_]): BeanMirror = {
    val symbol = currentMirror.classSymbol(clazz)

    //TODO: required in scala 2.10.0-M7 to make isCaseClass work (see https://issues.scala-lang.org/browse/SI-6277)
    // should be removed in later scala releases
    { symbol.typeSignature }

    if (symbol.isCaseClass)
      new BeanMirror(symbol)
    else
      throw new IllegalArgumentException("Only case classes are accepted as beans")
  }
}