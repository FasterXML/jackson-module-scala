package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.databind.JacksonModule.SetupContext
import com.fasterxml.jackson.databind.{DeserializationConfig, JavaType}
import com.fasterxml.jackson.module.scala.JacksonModule.InitializerBuilder
import com.fasterxml.jackson.module.scala.ScalaModule
import com.fasterxml.jackson.module.scala.introspect.OrderingLocator
import com.fasterxml.jackson.module.scala.modifiers.ScalaTypeModifierModule

import scala.collection._

trait SortedSetDeserializerModule extends ScalaTypeModifierModule {

  override def getInitializers(config: ScalaModule.Config): scala.Seq[SetupContext => Unit] = {
    super.getInitializers(config) ++ {
      val builder = new InitializerBuilder()
      builder += (_ addDeserializers new GenericFactoryDeserializerResolver[SortedSet, SortedIterableFactory](config) {

        override val CLASS_DOMAIN: Class[Collection[_]] = classOf[SortedSet[_]]

        override val factories: Iterable[(Class[_], Factory)] = sortFactories(Vector(
          (classOf[SortedSet[_]], SortedSet.asInstanceOf[Factory]),
          (classOf[immutable.TreeSet[_]], immutable.TreeSet.asInstanceOf[Factory]),
          (classOf[immutable.SortedSet[_]], immutable.SortedSet.asInstanceOf[Factory]),
          (classOf[mutable.TreeSet[_]], mutable.TreeSet.asInstanceOf[Factory]),
          (classOf[mutable.SortedSet[_]], mutable.SortedSet.asInstanceOf[Factory])
        ))

        override def builderFor[A](cf: Factory, valueType: JavaType): Builder[A] =
          cf.newBuilder[A](OrderingLocator.locate(valueType).asInstanceOf[Ordering[A]])

        override def hasDeserializerFor(deserializationConfig: DeserializationConfig, valueType: Class[_]): Boolean = {
          // TODO add implementation
          false
        }
      })
      builder.build()
    }
  }
}

object SortedSetDeserializerModule extends SortedSetDeserializerModule
