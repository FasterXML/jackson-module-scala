package com.fasterxml.jackson.module.scala.deser

import org.codehaus.jackson.JsonParser
import org.codehaus.jackson.`type`.JavaType
import org.codehaus.jackson.map.{DeserializationConfig, TypeDeserializer, DeserializationContext, JsonDeserializer}
import java.util.{Iterator, AbstractCollection, ArrayList}
import collection.mutable
import collection.immutable.Queue
import collection.generic.GenericCompanion
import org.codehaus.jackson.map.deser.{CollectionDeserializer, ContainerDeserializer}

private class BuilderWrapper[E](val builder: mutable.Builder[E, _ <: Seq[E]]) extends AbstractCollection[E] {

  override def add(e: E) = { builder += e; true }

  // Required by AbstractCollection, but the deserializer doesn't care about them.
  override def iterator(): Iterator[E] = null
  override def size(): Int = 0
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

private [deser] class SeqDeserializer(
    collectionType: JavaType,
    config: DeserializationConfig,
    valueDeser: JsonDeserializer[_],
    valueTypeDeser: TypeDeserializer)

  extends ContainerDeserializer[Seq[AnyRef]](classOf[SeqDeserializer]) {

  // This type is never used because we never let the collection deserializer construct an instance
  // TODO: Rework BuilderWrapper so it can serve in its place
  private val dummyCollectionClass =
    classOf[ArrayList[Object]].asInstanceOf[Class[java.util.Collection[Object]]]

  private val javaContainerType = config.constructType(dummyCollectionClass)
  private val javaCtor = dummyCollectionClass.getConstructor()
  private val containerDeserializer =
    new CollectionDeserializer(javaContainerType,valueDeser.asInstanceOf[JsonDeserializer[AnyRef]],valueTypeDeser,javaCtor)

  override def getContentType = containerDeserializer.getContentType

  override def getContentDeserializer = containerDeserializer.getContentDeserializer

  override def deserialize(jp: JsonParser, ctxt: DeserializationContext): Seq[AnyRef] = {
    val builder = SeqDeserializer.builderFor[AnyRef](collectionType.getRawClass)
    containerDeserializer.deserialize(jp, ctxt, new BuilderWrapper[AnyRef](builder))
    builder.result()
  }
}