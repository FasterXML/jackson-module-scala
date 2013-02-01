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

import com.thoughtworks.paranamer.BytecodeReadingParanamer
import scala.reflect.NameTransformer
import java.lang.reflect.{Constructor, Method}
import com.google.common.cache.{LoadingCache, CacheBuilderSpec, CacheLoader, CacheBuilder}

object BeanIntrospector {

  implicit def mkCacheLoader[K,V](f: (K) => V) = new CacheLoader[K,V] {
    def load(key: K) = f(key)
  }

  private [this] val paranamer = new BytecodeReadingParanamer
  private [this] val ctorParamNamesCache: LoadingCache[Constructor[_],Array[String]] =
    // TODO: consider module configuration of the cache
    CacheBuilder.newBuilder.maximumSize(50).build { ctor: Constructor[_] =>
      paranamer.lookupParameterNames(ctor).map(NameTransformer.decode(_))
    }

  def apply[T <: AnyRef](implicit mf: Manifest[_]): BeanDescriptor = apply[T](mf.erasure)

  def apply[T <: AnyRef](cls: Class[_]) = {

    def findConstructorParam(c: Class[_], name: String): Option[ConstructorParameter] = {
      if (c == classOf[AnyRef]) return None
      val primaryConstructor = c.getConstructors.headOption
      val debugCtorParamNames = primaryConstructor.map(ctorParamNamesCache(_)).getOrElse(Array.empty)
      val index = debugCtorParamNames.indexOf(name)
      if (index < 0) findConstructorParam(c.getSuperclass, name)
      else Some(ConstructorParameter(primaryConstructor.get, index, None))
    }

    lazy val hierarchy: Seq[Class[_]] = {
      def next(c: Class[_]): Seq[Class[_]] =
        if (c == null) Nil
        else if (c == classOf[AnyRef]) Nil
        else next(c.getSuperclass) :+ c
      next(cls)
    }

    def findMethod(cls: Class[_], name: String): Option[Method] = cls match {
      case null => None
      case c if c == classOf[AnyRef] => None
      case c => c.getDeclaredMethods.find(m => NameTransformer.decode(m.getName) == name) orElse findMethod(c.getSuperclass, name)
    }

    val fields = for {
      cls <- hierarchy
      field <- cls.getDeclaredFields
      name = NameTransformer.decode(field.getName)

      if !name.contains('$')
      if !field.isSynthetic

      param = findConstructorParam(cls, name)
      getter = findMethod(cls, name)
      setter = findMethod(cls, name + "_=")
    } yield PropertyDescriptor(name, param, Some(field), getter, setter)

    val methods = for {
      cls <- hierarchy
      getter <- cls.getDeclaredMethods
      name = NameTransformer.decode(getter.getName)

      if getter.getParameterTypes.length == 0
      if getter.getReturnType != Void.TYPE
      if !name.contains('$')
      if !fields.exists(_.name == name)
      setter <- findMethod(cls, name + "_=")
    } yield PropertyDescriptor(name, None, None, Some(getter), Some(setter))

    BeanDescriptor(cls, fields ++ methods)
  }

}
