package __foursquare_shaded__.com.fasterxml.jackson.module.scala.deser

import __foursquare_shaded__.com.fasterxml.jackson.databind._
import __foursquare_shaded__.com.fasterxml.jackson.module.scala.introspect.OrderingLocator
import __foursquare_shaded__.com.fasterxml.jackson.module.scala.modifiers.MapTypeModifierModule
import __foursquare_shaded__.com.fasterxml.jackson.module.scala.util.MapFactorySorter

import scala.collection._
import scala.language.existentials

trait SortedMapDeserializerModule extends MapTypeModifierModule {
  this += (_ addDeserializers new GenericMapFactoryDeserializerResolver[SortedMap, SortedMapFactory] {

    override val CLASS_DOMAIN: Class[Collection[_, _]] = classOf[SortedMap[_, _]]

    override val factories: List[(Class[_], Factory)] = new MapFactorySorter[Collection, SortedMapFactory]()
      .add(SortedMap)
      .add(immutable.SortedMap)
      .add(immutable.TreeMap)
      .add(mutable.SortedMap)
      .add(mutable.TreeMap)
      .toList

    override def builderFor[K, V](factory: Factory, keyType: JavaType, valueType: JavaType): Builder[K, V] =
      factory.newBuilder[K, V](OrderingLocator.locate[K](keyType))
  })
}
