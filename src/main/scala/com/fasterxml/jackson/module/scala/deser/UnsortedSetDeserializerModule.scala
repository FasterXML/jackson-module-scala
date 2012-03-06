package com.fasterxml.jackson.module.scala.deser

import org.codehaus.jackson.JsonParser
import org.codehaus.jackson.map.deser.ValueInstantiator
import org.codehaus.jackson.map.deser.std.{CollectionDeserializer, ContainerDeserializerBase}
import scala.collection.generic.GenericCompanion
import java.util.AbstractCollection
import scala.collection.{immutable, mutable}
import com.fasterxml.jackson.module.scala.modifiers.SetTypeModifierModule
import org.codehaus.jackson.map.`type`.CollectionLikeType
import org.codehaus.jackson.map.{BeanDescription, BeanProperty, DeserializerProvider, DeserializationContext, TypeDeserializer, JsonDeserializer, DeserializationConfig, Deserializers}

private class SetBuilderWrapper[E](val builder: mutable.Builder[E, _ <: collection.Set[E]]) extends AbstractCollection[E] {

  override def add(e: E) = { builder += e; true }

  // Required by AbstractCollection, but the deserializer doesn't care about them.
  def iterator() = null
  def size() = 0
}

private object UnsortedSetDeserializer {
  // This is a key component of making the type matching work.
  // Order matters, as derived classes must come before base classes.
  // TODO: try and make this lookup less painful-looking
  val COMPANIONS = List[(Class[_], GenericCompanion[collection.Set])](
    classOf[mutable.LinkedHashSet[_]] -> mutable.LinkedHashSet,
    classOf[mutable.HashSet[_]] -> mutable.HashSet,
    classOf[mutable.Set[_]] -> mutable.Set,
    classOf[immutable.ListSet[_]] -> immutable.ListSet,
    classOf[immutable.HashSet[_]] -> immutable.HashSet,
    classOf[immutable.Set[_]] -> immutable.Set
  )

  def companionFor(cls: Class[_]): GenericCompanion[collection.Set] =
    COMPANIONS find { _._1.isAssignableFrom(cls) } map { _._2 } getOrElse (Set)

  def builderFor[A](cls: Class[_]): mutable.Builder[A, collection.Set[A]] = companionFor(cls).newBuilder[A]
}

private class UnsortedSetDeserializer(collectionType: CollectionLikeType,
                                      config: DeserializationConfig,
                                      valueDeser: JsonDeserializer[_],
                                      valueTypeDeser: TypeDeserializer)

  extends ContainerDeserializerBase[collection.Set[_]](classOf[UnsortedSetDeserializer]) {

  private val javaContainerType = config.constructType(classOf[SetBuilderWrapper[AnyRef]])

  private val instantiator = new ValueInstantiator {
    def getValueTypeDesc = collectionType.getRawClass.getCanonicalName

    override def canCreateUsingDefault = true

    override def createUsingDefault =
      new SetBuilderWrapper[AnyRef](UnsortedSetDeserializer.builderFor[AnyRef](collectionType.getRawClass))
  }
  private val containerDeserializer =
    new CollectionDeserializer(javaContainerType, valueDeser.asInstanceOf[JsonDeserializer[AnyRef]], valueTypeDeser, instantiator)

  override def getContentType = containerDeserializer.getContentType

  override def getContentDeserializer = containerDeserializer.getContentDeserializer

  override def deserialize(jp: JsonParser, ctxt: DeserializationContext): collection.Set[_] =
    containerDeserializer.deserialize(jp, ctxt) match {
      case wrapper: SetBuilderWrapper[_] => wrapper.builder.result()
    }
}

private object UnsortedSetDeserializerResolver extends Deserializers.Base {

  override def findCollectionLikeDeserializer(collectionType: CollectionLikeType,
                                              config: DeserializationConfig,
                                              provider: DeserializerProvider,
                                              beanDesc: BeanDescription,
                                              property: BeanProperty,
                                              elementTypeDeserializer: TypeDeserializer,
                                              elementDeserializer: JsonDeserializer[_]): JsonDeserializer[_] = {
    val rawClass = collectionType.getRawClass

    if (classOf[collection.Set[_]].isAssignableFrom(rawClass)) {
      val resolvedDeserializer =
        Option(elementDeserializer).getOrElse(provider.findValueDeserializer(config, collectionType.containedType(0), property))
      new UnsortedSetDeserializer(collectionType, config, resolvedDeserializer, elementTypeDeserializer)
    } else {
      null
    }
  }
}

trait UnsortedSetDeserializerModule extends SetTypeModifierModule {
  this += UnsortedSetDeserializerResolver
}