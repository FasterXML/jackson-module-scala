package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.`type`.MapLikeType
import com.fasterxml.jackson.databind.deser.{ContextualDeserializer, Deserializers, ValueInstantiator}
import com.fasterxml.jackson.databind.deser.std.{ContainerDeserializerBase, MapDeserializer, StdValueInstantiator}
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.module.scala.{DefaultScalaModule, IteratorModule}

import scala.collection.immutable.IntMap
import scala.collection.{Map, mutable}
import scala.languageFeature.postfixOps

/**
 * Adds support for deserializing Scala [[scala.collection.immutable.IntMap]]s. Scala IntMaps can already be
 * serialized using [[IteratorModule]] or [[DefaultScalaModule]].
 *
 * @since 2.14.0
 */
class ImmutableIntMapDeserializerResolver extends Deserializers.Base {
  override def findMapLikeDeserializer(theType: MapLikeType,
                                       config: DeserializationConfig,
                                       beanDesc: BeanDescription,
                                       keyDeserializer: KeyDeserializer,
                                       elementTypeDeserializer: TypeDeserializer,
                                       elementDeserializer: JsonDeserializer[_]): JsonDeserializer[_] = {
    if (!classOf[IntMap[_]].isAssignableFrom(theType.getRawClass)) None.orNull
    else {
      new IntMapDeserializer(theType, new Instantiator(config, theType), keyDeserializer, elementDeserializer, elementTypeDeserializer)
    }
  }

  private class Instantiator(config: DeserializationConfig, mapType: MapLikeType) extends StdValueInstantiator(config, mapType) {
    override def canCreateUsingDefault = true

    override def createUsingDefault(ctxt: DeserializationContext) =
      new BuilderWrapper[AnyRef](IntMap.newBuilder[AnyRef])
  }

  private class BuilderWrapper[V >: AnyRef](val builder: mutable.Builder[Int, V]) extends java.util.AbstractMap[Int, V] {
    private var baseMap: Map[Any, V] = Map.empty

    override def put(k: Int, v: V): V = {
      builder += ((k, v));
      v
    }

    // Used by the deserializer when using readerForUpdating
    override def get(key: Any): V = baseMap.get(key).orNull

    // Isn't used by the deserializer
    override def entrySet(): java.util.Set[java.util.Map.Entry[Int, V]] = throw new UnsupportedOperationException

    def setInitialValue(init: Map[Int, V]): Unit = {
      init.foreach(Function.tupled(put))
      baseMap = init.asInstanceOf[Map[Any, V]]
    }
  }

  private class IntMapDeserializer[V](mapType: MapLikeType, containerDeserializer: MapDeserializer)
    extends ContainerDeserializerBase[IntMap[V]](mapType) with ContextualDeserializer {

    def this(mapType: MapLikeType, valueInstantiator: ValueInstantiator, keyDeser: KeyDeserializer, valueDeser: JsonDeserializer[_], valueTypeDeser: TypeDeserializer) = {
      this(mapType, new MapDeserializer(mapType, valueInstantiator, keyDeser, valueDeser.asInstanceOf[JsonDeserializer[AnyRef]], valueTypeDeser))
    }

    override def getContentType: JavaType = containerDeserializer.getContentType

    override def getContentDeserializer: JsonDeserializer[AnyRef] = containerDeserializer.getContentDeserializer

    override def createContextual(ctxt: DeserializationContext, property: BeanProperty): JsonDeserializer[_] = {
      val newDelegate = containerDeserializer.createContextual(ctxt, property).asInstanceOf[MapDeserializer]
      new IntMapDeserializer(mapType, newDelegate)
    }

    override def deserialize(jp: JsonParser, ctxt: DeserializationContext): IntMap[V] = {
      containerDeserializer.deserialize(jp, ctxt) match {
        case wrapper: BuilderWrapper[_] => wrapper.builder.result().asInstanceOf[IntMap[V]]
      }
    }

    override def deserialize(jp: JsonParser, ctxt: DeserializationContext, intoValue: IntMap[V]): IntMap[V] = {
      val bw = newBuilderWrapper(ctxt)
      bw.setInitialValue(intoValue.asInstanceOf[Map[Int, AnyRef]])
      containerDeserializer.deserialize(jp, ctxt, bw) match {
        case wrapper: BuilderWrapper[_] => wrapper.builder.result().asInstanceOf[IntMap[V]]
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
