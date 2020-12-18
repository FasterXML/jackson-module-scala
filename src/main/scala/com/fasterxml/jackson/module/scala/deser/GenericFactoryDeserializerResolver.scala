package com.fasterxml.jackson.module.scala.deser

import java.util
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.`type`.CollectionLikeType
import com.fasterxml.jackson.databind.deser.std.{CollectionDeserializer, ContainerDeserializerBase, StdValueInstantiator}
import com.fasterxml.jackson.databind.deser.{ContextualDeserializer, Deserializers, ValueInstantiator}
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.language.higherKinds

abstract class GenericFactoryDeserializerResolver[CC[_], CF[X[_]]] extends Deserializers.Base {
  type Collection[A] = CC[A]
  type Factory = CF[CC]
  type Builder[A] = mutable.Builder[A, _ <: Collection[A]]

  // Subclasses need to implement the following:
  val CLASS_DOMAIN: Class[_]
  val factories: Iterable[(Class[_], Factory)]
  def builderFor[A](cf: Factory, valueType: JavaType): Builder[A]

  def builderFor[A](cls: Class[_], valueType: JavaType): Builder[A] = factories
    .find(_._1.isAssignableFrom(cls))
    .map(_._2)
    .map(builderFor[A](_, valueType))
    .getOrElse(throw new IllegalStateException(s"Could not find deserializer for ${cls.getCanonicalName}. File issue on github:fasterxml/jackson-scala-module."))

  override def findCollectionLikeDeserializer(collectionType: CollectionLikeType,
                                              config: DeserializationConfig,
                                              beanDesc: BeanDescription,
                                              elementTypeDeserializer: TypeDeserializer,
                                              elementDeserializer: JsonDeserializer[_]): JsonDeserializer[_] = {
    if (!CLASS_DOMAIN.isAssignableFrom(collectionType.getRawClass)) None.orNull
    else {
      val deser = elementDeserializer.asInstanceOf[JsonDeserializer[AnyRef]]
      val instantiator = new Instantiator(config, collectionType, collectionType.getContentType)
      new Deserializer(collectionType, deser, elementTypeDeserializer, instantiator)
    }
  }

  protected def sortFactories(factories: IndexedSeq[(Class[_], Factory)]): Seq[(Class[_], Factory)] = {
    val cs = factories.toArray
    val output = new ListBuffer[(Class[_], Factory)]()

    val remaining = cs.map(_ => 1)
    val adjMatrix = Array.ofDim[Int](cs.length, cs.length)

    // Build the adjacency matrix. Only mark the in-edges.
    for (i <- cs.indices; j <- cs.indices) {
      val (ic, _) = cs(i)
      val (jc, _) = cs(j)

      if (i != j && ic.isAssignableFrom(jc)) {
        adjMatrix(i)(j) = 1
      }
    }

    // While we haven't removed every node, remove all nodes with 0 degree in-edges.
    while (output.length < cs.length) {
      val startLength = output.length

      for (i <- cs.indices) {
        if (remaining(i) == 1 && dotProduct(adjMatrix(i), remaining) == 0) {
          output += factories(i)
          remaining(i) = 0
        }
      }

      // If we couldn't remove any nodes, it means we've found a cycle. Realistically this should never happen.
      if (output.length == startLength) {
        throw new IllegalStateException("Companions contain a cycle.")
      }
    }

    output.toSeq
  }

  private def dotProduct(a: Array[Int], b: Array[Int]): Int = {
    if (a.length != b.length) throw new IllegalArgumentException()

    a.indices.map(i => a(i) * b(i)).sum
  }

  private class BuilderWrapper[A](val builder: Builder[A]) extends util.AbstractCollection[A] {
    var size = 0

    override def add(e: A): Boolean = { builder += e; size += 1; true }

    // Required by AbstractCollection, but not implemented
    override def iterator(): util.Iterator[A] = None.orNull

    def setInitialValue(init: Collection[A]): Unit = init.asInstanceOf[Iterable[A]].foreach(add)
  }

  private class Instantiator(config: DeserializationConfig, collectionType: JavaType, valueType: JavaType)
    extends StdValueInstantiator(config, collectionType) {

    override def canCreateUsingDefault = true

    override def createUsingDefault(ctxt: DeserializationContext) =
      new BuilderWrapper[AnyRef](builderFor[AnyRef](collectionType.getRawClass, valueType))
  }

  private class Deserializer[A](collectionType: JavaType, containerDeserializer: CollectionDeserializer)
    extends ContainerDeserializerBase[CC[A]](collectionType)
      with ContextualDeserializer {

    def this(collectionType: JavaType, valueDeser: JsonDeserializer[Object], valueTypeDeser: TypeDeserializer, valueInstantiator: ValueInstantiator) = {
      this(collectionType, new CollectionDeserializer(collectionType, valueDeser, valueTypeDeser, valueInstantiator))
    }

    override def createContextual(ctxt: DeserializationContext, property: BeanProperty): Deserializer[A] = {
      val newDelegate = containerDeserializer.createContextual(ctxt, property)
      new Deserializer(collectionType, newDelegate)
    }

    override def getContentType: JavaType = containerDeserializer.getContentType

    override def getContentDeserializer: JsonDeserializer[AnyRef] = containerDeserializer.getContentDeserializer

    override def deserialize(jp: JsonParser, ctxt: DeserializationContext): CC[A] = {
      containerDeserializer.deserialize(jp, ctxt) match {
        case wrapper: BuilderWrapper[_] => wrapper.builder.result().asInstanceOf[CC[A]]
      }
    }

    override def deserialize(jp: JsonParser, ctxt: DeserializationContext, intoValue: CC[A]): CC[A] = {
      val bw = newBuilderWrapper(ctxt)
      bw.setInitialValue(intoValue.asInstanceOf[CC[AnyRef]])
      containerDeserializer.deserialize(jp, ctxt, bw) match {
        case wrapper: BuilderWrapper[_] => wrapper.builder.result().asInstanceOf[CC[A]]
      }
    }

    override def getEmptyValue(ctxt: DeserializationContext): Object = {
      val bw = newBuilderWrapper(ctxt)
      bw.builder.result().asInstanceOf[Object]
    }

    private def newBuilderWrapper(ctxt: DeserializationContext): BuilderWrapper[AnyRef] = {
      containerDeserializer.getValueInstantiator.createUsingDefault(ctxt).asInstanceOf[BuilderWrapper[AnyRef]]
    }
  }
}
