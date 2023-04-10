package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.databind.{BeanDescription, DeserializationConfig, JavaType, JsonDeserializer}
import com.fasterxml.jackson.databind.`type`.CollectionLikeType
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer
import com.fasterxml.jackson.module.scala.modifiers.ScalaTypeModifierModule

import scala.collection._

trait UnsortedSetDeserializerModule extends ScalaTypeModifierModule {
  override def getModuleName: String = "UnsortedSetDeserializerModule"
  this += (_ addDeserializers new GenericFactoryDeserializerResolver[Set, IterableFactory] {

    override val CLASS_DOMAIN: Class[Collection[_]] = classOf[Set[_]]
    private val IGNORE_CLASS_DOMAIN: Class[_] = classOf[SortedSet[_]]

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

    override def findCollectionLikeDeserializer(collectionType: CollectionLikeType,
                                                config: DeserializationConfig,
                                                beanDesc: BeanDescription,
                                                elementTypeDeserializer: TypeDeserializer,
                                                elementDeserializer: JsonDeserializer[_]): JsonDeserializer[_] = {
      val rawClass = collectionType.getRawClass
      if (IGNORE_CLASS_DOMAIN.isAssignableFrom(rawClass)) {
        None.orNull
      } else {
        super.findCollectionLikeDeserializer(collectionType,
          config, beanDesc, elementTypeDeserializer, elementDeserializer)
      }
    }
  })
}
