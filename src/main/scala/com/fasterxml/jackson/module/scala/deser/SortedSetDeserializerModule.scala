package com.fasterxml.jackson.module.scala.deser

import java.util

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.`type`.CollectionLikeType
import com.fasterxml.jackson.databind.deser.std.{CollectionDeserializer, ContainerDeserializerBase, StdValueInstantiator}
import com.fasterxml.jackson.databind.deser.{ContextualDeserializer, Deserializers, ValueInstantiator}
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer
import com.fasterxml.jackson.module.scala.introspect.OrderingLocator
import com.fasterxml.jackson.module.scala.modifiers.SetTypeModifierModule

import scala.collection.{SortedIterableFactory, SortedSet, immutable, mutable}
import scala.language.postfixOps

private class SortedSetBuilderWrapper[E](val builder: mutable.Builder[E, _ <: collection.SortedSet[E]]) extends util.AbstractCollection[E] {

  override def add(e: E): Boolean = { builder += e; true }

  // Required by AbstractCollection, but the deserializer doesn't care about them.
  override def iterator(): util.Iterator[E]  = null
  override def size() = 0
}

private object SortedSetDeserializer {
  type BuilderFactory = Ordering[AnyRef] => mutable.Builder[AnyRef, SortedSet[AnyRef]]

  def lookupClass(s: String): Option[Class[_]] = try {
    Some(Predef.getClass.getClassLoader.loadClass(s))
  } catch {
    case e: ClassNotFoundException => None
  }

  def lookupBuilder(s: String): BuilderFactory = {
    val moduleClass = lookupClass(s + "$").get
    val module = moduleClass.getField("MODULE$").get(null).asInstanceOf[SortedIterableFactory[SortedSet]]
    o => module.newBuilder(o)
  }

  def classAndBuilder(s: String): Option[(Class[_], BuilderFactory)] = {
    lookupClass(s).map(c => c -> lookupBuilder(s))
  }

  val BUILDERS: mutable.LinkedHashMap[Class[_], BuilderFactory] = {
    val builder = mutable.LinkedHashMap.newBuilder[Class[_], BuilderFactory]
    builder += (classOf[mutable.TreeSet[_]] -> (mutable.TreeSet.newBuilder[AnyRef](_)))
    builder += (classOf[mutable.SortedSet[_]] -> (mutable.SortedSet.newBuilder[AnyRef](_)))
    builder += (classOf[immutable.TreeSet[_]] -> (immutable.TreeSet.newBuilder[AnyRef](_)))
    builder += (classOf[SortedSet[_]] -> (SortedSet.newBuilder[AnyRef](_)))
    builder.result()
  }

  def builderFor(cls: Class[_], valueType: JavaType): mutable.Builder[AnyRef, SortedSet[AnyRef]] = {
    val ordering = OrderingLocator.locate(valueType)
    val found: Option[BuilderFactory] = BUILDERS.find(_._1.isAssignableFrom(cls)).map(_._2)
    if (found.isDefined) found.get(ordering)
    else throw new IllegalArgumentException(cls.getCanonicalName + " is not a supported SortedSet")
  }
}

private class SortedSetInstantiator(config: DeserializationConfig, collectionType: JavaType, valueType: JavaType)
  extends StdValueInstantiator(config, collectionType) {

  override def canCreateUsingDefault = true

  override def createUsingDefault(ctxt: DeserializationContext) =
    new SortedSetBuilderWrapper[AnyRef](SortedSetDeserializer.builderFor(collectionType.getRawClass, valueType))
}


private class SortedSetDeserializer(collectionType: JavaType, containerDeserializer: CollectionDeserializer)
  extends ContainerDeserializerBase[collection.SortedSet[_]](collectionType)
  with ContextualDeserializer
{
  def this(collectionType: JavaType, valueDeser: JsonDeserializer[Object], valueTypeDeser: TypeDeserializer, valueInstantiator: ValueInstantiator) =
    this(collectionType, new CollectionDeserializer(collectionType, valueDeser, valueTypeDeser, valueInstantiator))

  def createContextual(ctxt: DeserializationContext, property: BeanProperty): SortedSetDeserializer = {
    val newDelegate = containerDeserializer.createContextual(ctxt, property)
    new SortedSetDeserializer(collectionType, newDelegate)
  }

  override def getContentType: JavaType = containerDeserializer.getContentType

  override def getContentDeserializer: JsonDeserializer[AnyRef] = containerDeserializer.getContentDeserializer

  override def deserialize(jp: JsonParser, ctxt: DeserializationContext): collection.SortedSet[_] =
    containerDeserializer.deserialize(jp, ctxt) match {
      case wrapper: SortedSetBuilderWrapper[_] => wrapper.builder.result()
    }
}

private object SortedSetDeserializerResolver extends Deserializers.Base {
  private final val SORTED_SET = classOf[collection.SortedSet[_]]

  override def findCollectionLikeDeserializer(collectionType: CollectionLikeType,
                                              config: DeserializationConfig,
                                              beanDesc: BeanDescription,
                                              elementTypeDeserializer: TypeDeserializer,
                                              elementDeserializer: JsonDeserializer[_]): JsonDeserializer[_] = {
    val rawClass = collectionType.getRawClass

    if (!SORTED_SET.isAssignableFrom(rawClass)) null
    else {
      val deser = elementDeserializer.asInstanceOf[JsonDeserializer[AnyRef]]
      val instantiator = new SortedSetInstantiator(config, collectionType, collectionType.getContentType)
      new SortedSetDeserializer(collectionType, deser, elementTypeDeserializer, instantiator)
    }
  }
}

trait SortedSetDeserializerModule extends SetTypeModifierModule {
  this += (_ addDeserializers SortedSetDeserializerResolver)
}
