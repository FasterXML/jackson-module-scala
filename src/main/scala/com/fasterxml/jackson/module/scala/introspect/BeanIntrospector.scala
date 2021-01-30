/*
 * Derived from source code of scalabeans:
 * https://raw.github.com/scalastuff/scalabeans/62b50c4e2482cbc1f494e0ac5c6c54fadc1bbcdd/src/main/scala/org/scalastuff/scalabeans/BeanIntrospector.scala
 *
 * The scalabeans code is covered by the copyright statement that follows.
 */

/*
 * Copyright (c) 2011 ScalaStuff.org (joint venture of Alexander Dvorkovyy and Ruud Diterwich)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.fasterxml.jackson.module.scala.introspect

import java.lang.reflect.{Constructor, Field, Method, Modifier}
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.scala.util.ClassW

import scala.annotation.tailrec
import scala.reflect.NameTransformer

//TODO: This might be more efficient/type safe if we used Scala reflection here
//but we have to support 2.9.x and 2.10.x - once the scala reflection APIs
//settle down, it might be a good idea. Using java.lang.reflect.* means we have to
//suffer with type erasure, so we can only compare gross types of arguments and return values

object BeanIntrospector {

  private def getCtorParams(ctor: Constructor[_]): Seq[String] = {
    val names = JavaParameterIntrospector.getCtorParamNames(ctor)
    names.map(NameTransformer.decode)
  }

  def apply[T <: AnyRef](implicit mf: Manifest[_]): BeanDescriptor = apply[T](mf.runtimeClass)

  def apply[T <: AnyRef](cls: Class[_]) = {

    /**
     * Find the index of the named parameter in the primary constructor of the mentioned class.
     */
    @tailrec
    def findConstructorParam(c: Class[_], name: String): Option[ConstructorParameter] = {
      //c can be null if we're asked for the superclass of Object or AnyRef
      if (c == null || c == classOf[AnyRef]) {
        None
      } else {
        //try to use constructor with most parameters to avoid https://github.com/FasterXML/jackson-module-scala/issues/330
        val primaryConstructor = c.getConstructors.sortBy(c => -c.getParameterCount).headOption
        val debugCtorParamNames = primaryConstructor.toIndexedSeq.flatMap(getCtorParams)
        val index = debugCtorParamNames.indexOf(name)
        val companion = findCompanionObject(c)
        if (index >= 0) {
          Some(ConstructorParameter(primaryConstructor.get, index, findConstructorDefaultValue(companion, index)))
        } else {
          findConstructorParam(c.getSuperclass, name)
        }
      }
    }

    def findConstructorDefaultValue(maybeCompanion: Option[AnyRef], index: Int): Option[() => AnyRef] = {
      val methodName = "$lessinit$greater$default$" + (index + 1)
      maybeCompanion.flatMap(companion => companion.getClass.getMethods.collectFirst {
        case method if method.getName == methodName && method.getParameterTypes.length == 0 =>
          () => method.invoke(companion)
      })
    }

    def findCompanionObject(c: Class[_]): Option[AnyRef] = {
      try {
        Some(c.getClassLoader.loadClass(c.getName + "$").getDeclaredField("MODULE$").get(null))
      } catch {
        case e: Exception => None
      }
    }

    val hierarchy: Seq[Class[_]] = {
      @tailrec
      def next(c: Class[_], acc: List[Class[_]]): List[Class[_]] = {
        if (c == null || c == classOf[AnyRef]) {
          acc
        } else {
          next(c.getSuperclass, acc :+ c)
        }
      }
      next(cls, Nil)
    }

    def listMethods(cls: Class[_]): Stream[Method] = cls match {
      case null => Stream.empty
      case c if c == classOf[AnyRef] => Stream.empty
      case c => c.getDeclaredMethods.toStream #::: listMethods(c.getSuperclass)
    }

    def isNotSyntheticOrBridge(m: Method): Boolean = !(m.isBridge || m.isSynthetic)

    def findMethod(cls: Class[_], name: String): Seq[Method] =
      listMethods(cls).filter(isNotSyntheticOrBridge).filter(m => NameTransformer.decode(m.getName) == name)

    def listFields(cls: Class[_]): Stream[Field] = cls match {
      case null => Stream.empty
      case c if c == classOf[AnyRef] => Stream.empty
      case c => c.getDeclaredFields.toStream #::: listFields(c.getSuperclass)
    }

    def findField(cls: Class[_], fieldName: String): Option[Field] =
      listFields(cls).filter(!_.isSynthetic).find(f => NameTransformer.decode(f.getName) == fieldName).headOption

    def isAcceptableField(field: Field): Boolean = {
      //TODO: check for private/public? Do we care?
      val modifiers = field.getModifiers
      !(Modifier.isStatic(modifiers) || Modifier.isVolatile(modifiers) || Modifier.isTransient(modifiers) || field.isSynthetic)
    }

    //ignore static methods - we can actually have one of these if a class
    //inherits from a java class
    def isAcceptableMethod(method: Method): Boolean = {
      //can't be static, a bridge or synthetic method (which are generated by the compiler)
      !Modifier.isStatic(method.getModifiers) && isNotSyntheticOrBridge(method)
    }

    def findGetter(cls: Class[_], propertyName: String): Option[Method] = {
      findMethod(cls, propertyName).find(isAcceptableGetter)
    }

    def findBeanGetter(cls: Class[_], propertyName: String): Option[Method] = {
      findMethod(cls, "get" + propertyName.capitalize).find(isAcceptableGetter)
    }

    //True if the method fits the 'getter' pattern
    def isAcceptableGetter(m: Method): Boolean = {
      isAcceptableMethod(m) && m.getParameterTypes.length == 0 && m.getReturnType != Void.TYPE
    }

    def findSetter(cls: Class[_], propertyName: String): Option[Method] = {
      findMethod(cls, propertyName + "_=").find(isAcceptableSetter)
    }

    def findBeanSetter(cls: Class[_], propertyName: String): Option[Method] = {
      findMethod(cls, "set" + propertyName.capitalize).find(isAcceptableSetter)
    }

    //True if the method fits the 'setter' pattern
    def isAcceptableSetter(m: Method): Boolean = {
      isAcceptableMethod(m) && m.getParameterTypes.length == 1 && m.getReturnType == Void.TYPE
    }

    val privateRegex = """(.*)\$\$(.*)""".r
    def maybePrivateName(field: Field): String = {
      val definedName = NameTransformer.decode(field.getName)

      val canonicalName = try {
        Option(field.getDeclaringClass.getCanonicalName)
      } catch {
        // This gets thrown in the REPL
        case e: InternalError => None
      }
      canonicalName.flatMap { cn =>
        val PrivateName = cn.replace('.','$')
        definedName match {
          case privateRegex(PrivateName, rest) => Some(rest)
          case _ => None
        }
      }.getOrElse(definedName)

    }

    def isScalaCaseObject(cls: Class[_]): Boolean = {
      ClassW.productClass.isAssignableFrom(cls) && cls.getName.endsWith("$")
    }

    //TODO - as we walk the classes, it would be nice to use a language specific introspector
    //for example, a scala class can inherit from a java class - when we're examining
    //a scala class, we should use the behavior defined here, but if the base class is a Java
    //class we should use the default JavaBean rules for determining fields, getters and setters.

    //TODO - this ignores the possibility that the types may disagree between the field, the getter and the setter.
    //Theoretically, the getter should have an identical return type as the field, and the setter should take
    //one argument of the same type as the field

    //create properties for all appropriate fields
    val fields = for {
      cls <- hierarchy
      classW = ClassW(cls)
      field <- cls.getDeclaredFields
      name = maybePrivateName(field)
      if !name.contains('$')
      if (classW.isScalaObject || isScalaCaseObject(cls) || isAcceptableField(field))
      beanGetter = findBeanGetter(cls, name)
      beanSetter = findBeanSetter(cls, name)
    } yield PropertyDescriptor(name, findConstructorParam(hierarchy.head, name), Some(field), findGetter(cls, name), findSetter(cls, name), beanGetter, beanSetter)

    //this will create properties for all methods with a non-Unit/Void return type and no arguments
    //that also have a setter present that matches the pattern 'propertyName'+'_='.
    //We also require that there is no field defined on the class matching the param name -
    //the assumption is that if we have made it this far, the field/getter/setter combo would have been
    //picked up if the field was valid. If we have not already added the field, and it's present, then
    //it must be somehow inappropriate (static, volitile, etc)
    //this catches the case where people explicitly write getter-like and setter-like methods
    //(for example, compound properties)
    //Note - this will discard 'dangling' getters as an introspected property - getters must have setters,
    //and also the reverse.
    //TODO: Ensure the getters and setters with matching names also have matching types
    val methods = for {
      cls <- hierarchy
      getter <- cls.getDeclaredMethods
      name = NameTransformer.decode(getter.getName)
      if isAcceptableGetter(getter)
      if findField(cls, name).isEmpty
      if !name.contains('$')
      if !fields.exists(_.name == name)
      getterProperty = getter.getAnnotation(classOf[JsonProperty])
      setter = findSetter(cls, name) if setter.isDefined || (getterProperty != null && getterProperty.value != JsonProperty.USE_DEFAULT_NAME)
      beanGetter = findBeanGetter(cls, name)
      beanSetter = findBeanSetter(cls, name)
    } yield PropertyDescriptor(name, None, None, Some(getter), setter, beanGetter, beanSetter)


    BeanDescriptor(cls, fields ++ methods)
  }

}
