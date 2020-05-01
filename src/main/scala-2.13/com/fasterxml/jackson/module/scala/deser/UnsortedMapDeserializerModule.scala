package __foursquare_shaded__.com.fasterxml.jackson.module.scala.deser

import __foursquare_shaded__.com.fasterxml.jackson.databind._
import __foursquare_shaded__.com.fasterxml.jackson.module.scala.modifiers.MapTypeModifierModule
import __foursquare_shaded__.com.fasterxml.jackson.module.scala.util.MapFactorySorter

import scala.collection._
import scala.language.existentials

trait UnsortedMapDeserializerModule extends MapTypeModifierModule {
  this += (_ addDeserializers new GenericMapFactoryDeserializerResolver[Map, MapFactory] {

    override val CLASS_DOMAIN: Class[Collection[_, _]] = classOf[Map[_, _]]

    override val factories: List[(Class[_], Factory)] = new MapFactorySorter[Collection, MapFactory]()
      .add(Map)
      .add(immutable.HashMap)
      .add(immutable.ListMap)
      .add(immutable.Map)
      .add(mutable.HashMap)
      .add(mutable.LinkedHashMap)
      .add(mutable.ListMap)
      .add(mutable.Map)
      .add(concurrent.TrieMap)
      // OpenHashMap is omitted due to deprecation
      // WeakHashMap is omitted due to the unlikely use case
      .toList

    override def builderFor[K, V](factory: Factory, keyType: JavaType, valueType: JavaType): Builder[K, V] = factory.newBuilder[K, V]
  })
}
