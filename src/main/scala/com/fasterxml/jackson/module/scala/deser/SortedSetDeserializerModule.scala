package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.databind.`type`.CollectionLikeType
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer
import com.fasterxml.jackson.databind.{BeanDescription, DeserializationConfig, JavaType, JsonDeserializer}
import com.fasterxml.jackson.module.scala.introspect.OrderingLocator
import com.fasterxml.jackson.module.scala.modifiers.ScalaTypeModifierModule

import scala.collection._

trait SortedSetDeserializerModule extends ScalaTypeModifierModule {
  override def getModuleName: String = "SortedSetDeserializerModule"
  this += (_ addDeserializers new GenericFactoryDeserializerResolver[SortedSet, SortedIterableFactory] {

    private val IMMUTABLE_BITSET_CLASS: Class[_] = classOf[immutable.BitSet]
    private val MUTABLE_BITSET_CLASS: Class[_] = classOf[mutable.BitSet]
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

    override def findCollectionLikeDeserializer(collectionType: CollectionLikeType,
                                                config: DeserializationConfig,
                                                beanDesc: BeanDescription,
                                                elementTypeDeserializer: TypeDeserializer,
                                                elementDeserializer: JsonDeserializer[_]): JsonDeserializer[_] = {
      val rawClass = collectionType.getRawClass
      if (IMMUTABLE_BITSET_CLASS.isAssignableFrom(rawClass)) {
        None.orNull
      } else if (MUTABLE_BITSET_CLASS.isAssignableFrom(rawClass)) {
        None.orNull
      } else {
        super.findCollectionLikeDeserializer(collectionType,
          config, beanDesc, elementTypeDeserializer, elementDeserializer)
      }
    }
  })
}
