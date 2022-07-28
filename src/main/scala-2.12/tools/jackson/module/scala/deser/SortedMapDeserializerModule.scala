package tools.jackson.module.scala.deser

import tools.jackson.databind.JacksonModule.SetupContext
import tools.jackson.databind._
import tools.jackson.module.scala.JacksonModule.InitializerBuilder
import tools.jackson.module.scala.ScalaModule
import tools.jackson.module.scala.introspect.OrderingLocator
import tools.jackson.module.scala.modifiers.MapTypeModifierModule

import scala.collection._
import scala.collection.generic.SortedMapFactory
import scala.language.existentials

trait SortedMapDeserializerModule extends MapTypeModifierModule {
  override def getInitializers(config: ScalaModule.Config): scala.Seq[SetupContext => Unit] = {
    super.getInitializers(config) ++ {
      val builder = new InitializerBuilder()
      builder += new GenericMapFactoryDeserializerResolver[SortedMap, SortedMapFactory](config) {

        override val CLASS_DOMAIN: Class[Collection[_, _]] = classOf[SortedMap[_, _]]

        override val factories: Iterable[(Class[_], Factory)] = sortFactories(Vector(
          (classOf[SortedMap[_, _]], SortedMap.asInstanceOf[Factory]),
          (classOf[immutable.SortedMap[_, _]], immutable.SortedMap.asInstanceOf[Factory]),
          (classOf[immutable.TreeMap[_, _]], immutable.TreeMap.asInstanceOf[Factory]),
          (classOf[mutable.SortedMap[_, _]], mutable.SortedMap.asInstanceOf[Factory]),
          (classOf[mutable.TreeMap[_, _]], mutable.TreeMap.asInstanceOf[Factory])
        ))

        override def builderFor[K, V](factory: Factory, keyType: JavaType, valueType: JavaType): Builder[K, V] =
          factory.newBuilder[K, V](OrderingLocator.locate[K](keyType))
      }
      builder.build()
    }
  }
}

object SortedMapDeserializerModule extends SortedMapDeserializerModule
