package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.databind.JacksonModule.SetupContext
import com.fasterxml.jackson.databind.{DeserializationConfig, JavaType}
import com.fasterxml.jackson.module.scala.JacksonModule.InitializerBuilder
import com.fasterxml.jackson.module.scala.ScalaModule
import com.fasterxml.jackson.module.scala.modifiers.ScalaTypeModifierModule

import scala.collection._

trait UnsortedSetDeserializerModule extends ScalaTypeModifierModule {
  override def getInitializers(config: ScalaModule.Config): scala.Seq[SetupContext => Unit] = {
    super.getInitializers(config) ++ {
      val builder = new InitializerBuilder()
      builder += new GenericFactoryDeserializerResolver[Set, IterableFactory](config) {

        override val CLASS_DOMAIN: Class[Collection[_]] = classOf[Set[_]]

        override val factories: Iterable[(Class[_], Factory)] = sortFactories(Vector(
          (classOf[Set[_]], Set.asInstanceOf[Factory]),
          (classOf[immutable.HashSet[_]], immutable.HashSet.asInstanceOf[Factory]),
          (classOf[immutable.ListSet[_]], immutable.ListSet.asInstanceOf[Factory]),
          (classOf[immutable.Set[_]], immutable.Set.asInstanceOf[Factory]),
          (classOf[mutable.HashSet[_]], mutable.HashSet.asInstanceOf[Factory]),
          (classOf[mutable.LinkedHashSet[_]], mutable.LinkedHashSet.asInstanceOf[Factory]),
          (classOf[mutable.Set[_]], mutable.Set.asInstanceOf[Factory])
        ))

        override def builderFor[A](cf: Factory, javaType: JavaType): Builder[A] = cf.newBuilder[A]

        override def hasDeserializerFor(deserializationConfig: DeserializationConfig, valueType: Class[_]): Boolean = {
          // TODO add implementation
          false
        }
      }
      builder.build()
    }
  }
}

object UnsortedSetDeserializerModule extends UnsortedSetDeserializerModule