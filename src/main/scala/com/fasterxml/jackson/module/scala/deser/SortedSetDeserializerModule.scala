package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.module.scala.modifiers.SetTypeModifierModule
import com.fasterxml.jackson.databind.deser.{ValueInstantiator, ContextualDeserializer, Deserializers}
import com.fasterxml.jackson.databind.deser.std.{StdValueInstantiator, CollectionDeserializer, ContainerDeserializerBase}
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer
import com.fasterxml.jackson.core.JsonParser
import scala.collection._
import com.fasterxml.jackson.databind.`type`.CollectionLikeType
import java.util.AbstractCollection
import com.fasterxml.jackson.module.scala.introspect.OrderingLocator
import java.lang.Object
import scala.collection.generic.SortedSetFactory
import scala.Some
import scala.collection.immutable
import scala.language.postfixOps

private class SortedSetBuilderWrapper[E](val builder: mutable.Builder[E, _ <: collection.SortedSet[E]]) extends AbstractCollection[E] {

  override def add(e: E) = { builder += e; true }

  // Required by AbstractCollection, but the deserializer doesn't care about them.
  def iterator() = null
  def size() = 0
}

private object SortedSetDeserializer {
  type BuilderFactory = (Ordering[AnyRef]) => mutable.Builder[AnyRef, SortedSet[AnyRef]]

  def lookupClass(s: String): Option[Class[_]] = try {
    Some(Predef.getClass.getClassLoader.loadClass(s))
  } catch {
    case e: ClassNotFoundException => None
  }

  def lookupBuilder(s: String): BuilderFactory = {
    val moduleClass = lookupClass(s + "$").get
    val module = moduleClass.getField("MODULE$").get(null).asInstanceOf[SortedSetFactory[SortedSet]]
    (o) => module.newBuilder(o)
  }

  def classAndBuilder(s: String): Option[(Class[_], BuilderFactory)] = {
    lookupClass(s).map(c => c -> lookupBuilder(s))
  }

  val BUILDERS = {
    val builder = mutable.LinkedHashMap.newBuilder[Class[_], BuilderFactory]

    // These were added in 2.10. We want to support them, but can't statically reference them, because
    // the 2.9 library doesn't include them, and multi-target builds are awkward.
    classAndBuilder("scala.collection.mutable.TreeSet").foreach(builder +=)
    classAndBuilder("scala.collection.mutable.SortedSet").foreach(builder +=)

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

private class SortedSetInstantiator(config: DeserializationConfig, collectionType: Class[_], valueType: JavaType)
  extends StdValueInstantiator(config, collectionType) {

  override def canCreateUsingDefault = true

  override def createUsingDefault(ctxt: DeserializationContext) =
    new SortedSetBuilderWrapper[AnyRef](SortedSetDeserializer.builderFor(collectionType, valueType))
}


private class SortedSetDeserializer(collectionType: JavaType, containerDeserializer: CollectionDeserializer)
  extends ContainerDeserializerBase[collection.SortedSet[_]](collectionType)
  with ContextualDeserializer
{
  def this(collectionType: JavaType, valueDeser: JsonDeserializer[Object], valueTypeDeser: TypeDeserializer, valueInstantiator: ValueInstantiator) =
    this(collectionType, new CollectionDeserializer(collectionType, valueDeser, valueTypeDeser, valueInstantiator))

  def createContextual(ctxt: DeserializationContext, property: BeanProperty) = {
    val newDelegate = containerDeserializer.createContextual(ctxt, property)
    new SortedSetDeserializer(collectionType, newDelegate)
  }

  override def getContentType = containerDeserializer.getContentType

  override def getContentDeserializer = containerDeserializer.getContentDeserializer

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
      val instantiator = new SortedSetInstantiator(config, rawClass, collectionType.getContentType)
      new SortedSetDeserializer(collectionType, deser, elementTypeDeserializer, instantiator)
    }
  }

}

trait SortedSetDeserializerModule extends SetTypeModifierModule {
  this += (_ addDeserializers SortedSetDeserializerResolver)
}
