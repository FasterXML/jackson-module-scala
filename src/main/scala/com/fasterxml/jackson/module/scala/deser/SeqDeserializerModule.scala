package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.module.scala.modifiers.SeqTypeModifierModule

import com.fasterxml.jackson.core.JsonParser.{JsonToken, JsonParser};

import com.fasterxml.jackson.databind._;
import com.fasterxml.jackson.databind.deser.{Deserializers, ValueInstantiator};
import com.fasterxml.jackson.databind.deser.std.{CollectionDeserializer, ContainerDeserializer};
import com.fasterxml.jackson.databind.jsontype.{TypeDeserializer};
import com.fasterxml.jackson.databind.`type`.CollectionLikeType;

import collection.mutable
import collection.generic.GenericCompanion
import collection.immutable.Queue

import java.util.AbstractCollection

private class BuilderWrapper[E](val builder: mutable.Builder[E, _ <: Seq[E]]) extends AbstractCollection[E] {

  override def add(e: E) = { builder += e; true }

  // Required by AbstractCollection, but the deserializer doesn't care about them.
  def iterator() = null
  def size() = 0
}

private object SeqDeserializer {
  // This hurts my eyes, but it's the key component of making the type matching work.
  // Also, order matters, as derived classes must come before base classes.
  // TODO: try and make this lookup less painful-looking
  val COMPANIONS = List[(Class[_],GenericCompanion[collection.Seq])](
    classOf[mutable.ResizableArray[_]] -> mutable.ResizableArray,
    classOf[mutable.ArraySeq[_]] -> mutable.ArraySeq,
    classOf[mutable.IndexedSeq[_]] -> mutable.IndexedSeq,
    classOf[IndexedSeq[_]] -> IndexedSeq,
    classOf[Stream[_]] -> Stream,
    classOf[Queue[_]] -> Queue,
    classOf[mutable.Queue[_]] -> mutable.Queue,
    classOf[mutable.MutableList[_]] -> mutable.MutableList,
    classOf[mutable.LinearSeq[_]] -> mutable.LinearSeq,
    classOf[mutable.ListBuffer[_]] -> mutable.ListBuffer,
    classOf[mutable.Buffer[_]] -> mutable.Buffer
  )

  def companionFor(cls: Class[_]): GenericCompanion[collection.Seq] =
    COMPANIONS find { _._1.isAssignableFrom(cls) } map { _._2 } getOrElse(Seq)

  def builderFor[A](cls: Class[_]): mutable.Builder[A,Seq[A]] = companionFor(cls).newBuilder[A]
}

private class SeqDeserializer(
    collectionType: JavaType,
    config: DeserializationConfig,
    valueDeser: JsonDeserializer[_],
    valueTypeDeser: TypeDeserializer)

  extends ContainerDeserializer[Seq[_]](classOf[SeqDeserializer]) {

  private val javaContainerType = config.constructType(classOf[BuilderWrapper[AnyRef]])

  private val instantiator = new ValueInstantiator {
    def getValueTypeDesc = collectionType.getRawClass.getCanonicalName

    override def canCreateUsingDefault = true

    override def createUsingDefault =
      new BuilderWrapper[AnyRef](SeqDeserializer.builderFor[AnyRef](collectionType.getRawClass))
  }
  private val containerDeserializer =
    new CollectionDeserializer(javaContainerType,valueDeser.asInstanceOf[JsonDeserializer[AnyRef]],valueTypeDeser,instantiator)

  override def getContentType = containerDeserializer.getContentType

  override def getContentDeserializer = containerDeserializer.getContentDeserializer

  override def deserialize(jp: JsonParser, ctxt: DeserializationContext): Seq[_] =
    containerDeserializer.deserialize(jp, ctxt) match {
      case wrapper: BuilderWrapper[_] => wrapper.builder.result()
    }
}

private object SeqDeserializerResolver extends Deserializers.Base {

  override def findCollectionLikeDeserializer(collectionType: CollectionLikeType,
                     config: DeserializationConfig,
                     provider: DeserializerProvider,
                     beanDesc: BeanDescription,
                     property: BeanProperty,
                     elementTypeDeserializer: TypeDeserializer,
                     elementDeserializer: JsonDeserializer[_]): JsonDeserializer[_] = {
    val rawClass = collectionType.getRawClass

    if (classOf[Seq[_]].isAssignableFrom(rawClass)) {
      val resolvedDeserializer =
        Option(elementDeserializer).getOrElse(provider.findValueDeserializer(config,collectionType.containedType(0),property))
      new SeqDeserializer(collectionType, config, resolvedDeserializer, elementTypeDeserializer)
    } else null
  }

}

trait SeqDeserializerModule extends SeqTypeModifierModule {
  this += SeqDeserializerResolver
}