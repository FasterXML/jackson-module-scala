package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.`type`.MapLikeType
import com.fasterxml.jackson.databind.deser.{ContextualDeserializer, Deserializers}
import com.fasterxml.jackson.databind.deser.std.{ContainerDeserializerBase, MapDeserializer, StdValueInstantiator}
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.module.scala.{DefaultScalaModule, IteratorModule}

import java.util
import scala.collection.{immutable, mutable}
import scala.collection.JavaConverters._

/**
 * Adds support for deserializing Scala [[scala.collection.immutable.LongMap]]s and [[scala.collection.mutable.LongMap]]s.
 * Scala LongMaps can already be serialized using [[IteratorModule]] or [[DefaultScalaModule]].
 *
 * @since 2.14.0
 */
private[deser] object LongMapDeserializerResolver extends Deserializers.Base {

  private val immutableLongMapClass = classOf[immutable.LongMap[_]]
  private val mutableLongMapClass = classOf[mutable.LongMap[_]]

  override def findMapLikeDeserializer(theType: MapLikeType,
                                       config: DeserializationConfig,
                                       beanDesc: BeanDescription,
                                       keyDeserializer: KeyDeserializer,
                                       elementTypeDeserializer: TypeDeserializer,
                                       elementDeserializer: JsonDeserializer[_]): JsonDeserializer[_] = {
    if (immutableLongMapClass.isAssignableFrom(theType.getRawClass)) {
      val mapDeserializer = new MapDeserializer(theType, new ImmutableLongMapInstantiator(config, theType), keyDeserializer,
        elementDeserializer.asInstanceOf[JsonDeserializer[AnyRef]], elementTypeDeserializer)
      new ImmutableLongMapDeserializer(theType, mapDeserializer)
    } else if (mutableLongMapClass.isAssignableFrom(theType.getRawClass)) {
      val mapDeserializer = new MapDeserializer(theType, new MutableLongMapInstantiator(config, theType), keyDeserializer,
        elementDeserializer.asInstanceOf[JsonDeserializer[AnyRef]], elementTypeDeserializer)
      new MutableLongMapDeserializer(theType, mapDeserializer)
    } else {
      None.orNull
    }
  }

  private class ImmutableLongMapDeserializer[V](mapType: MapLikeType, containerDeserializer: MapDeserializer)
    extends ContainerDeserializerBase[immutable.LongMap[V]](mapType) with ContextualDeserializer {

    override def getContentType: JavaType = containerDeserializer.getContentType

    override def getContentDeserializer: JsonDeserializer[AnyRef] = containerDeserializer.getContentDeserializer

    override def createContextual(ctxt: DeserializationContext, property: BeanProperty): JsonDeserializer[_] = {
      val newDelegate = containerDeserializer.createContextual(ctxt, property).asInstanceOf[MapDeserializer]
      new ImmutableLongMapDeserializer(mapType, newDelegate)
    }

    override def deserialize(jp: JsonParser, ctxt: DeserializationContext): immutable.LongMap[V] = {
      containerDeserializer.deserialize(jp, ctxt) match {
        case wrapper: ImmutableMapWrapper => wrapper.asLongMap()
      }
    }

    override def deserialize(jp: JsonParser, ctxt: DeserializationContext, intoValue: immutable.LongMap[V]): immutable.LongMap[V] = {
      val newMap = deserialize(jp, ctxt)
      if (newMap.isEmpty) {
        intoValue
      } else {
        intoValue ++ newMap
      }
    }

    override def getEmptyValue(ctxt: DeserializationContext): Object = immutable.LongMap.empty[V]
  }

  private class MutableLongMapDeserializer[V](mapType: MapLikeType, containerDeserializer: MapDeserializer)
    extends ContainerDeserializerBase[mutable.LongMap[V]](mapType) with ContextualDeserializer {

    override def getContentType: JavaType = containerDeserializer.getContentType

    override def getContentDeserializer: JsonDeserializer[AnyRef] = containerDeserializer.getContentDeserializer

    override def createContextual(ctxt: DeserializationContext, property: BeanProperty): JsonDeserializer[_] = {
      val newDelegate = containerDeserializer.createContextual(ctxt, property).asInstanceOf[MapDeserializer]
      new MutableLongMapDeserializer(mapType, newDelegate)
    }

    override def deserialize(jp: JsonParser, ctxt: DeserializationContext): mutable.LongMap[V] = {
      containerDeserializer.deserialize(jp, ctxt) match {
        case wrapper: MutableMapWrapper => wrapper.asLongMap()
      }
    }

    override def deserialize(jp: JsonParser, ctxt: DeserializationContext, intoValue: mutable.LongMap[V]): mutable.LongMap[V] = {
      val newMap = deserialize(jp, ctxt)
      if (newMap.isEmpty) {
        intoValue
      } else {
        intoValue ++ newMap
      }
    }

    override def getEmptyValue(ctxt: DeserializationContext): Object = mutable.LongMap.empty[V]
  }

  private class ImmutableLongMapInstantiator(config: DeserializationConfig, mapType: MapLikeType) extends StdValueInstantiator(config, mapType) {
    override def canCreateUsingDefault = true
    override def createUsingDefault(ctxt: DeserializationContext) = new ImmutableMapWrapper
  }

  private class MutableLongMapInstantiator(config: DeserializationConfig, mapType: MapLikeType) extends StdValueInstantiator(config, mapType) {
    override def canCreateUsingDefault = true

    override def createUsingDefault(ctxt: DeserializationContext) = new MutableMapWrapper
  }

  private class ImmutableMapWrapper extends util.AbstractMap[Object, Object] {
    var baseMap = immutable.LongMap[Object]()

    override def put(k: Object, v: Object): Object = {
      k match {
        case n: Number => {
          val l = n.longValue()
          val oldValue = baseMap.get(l)
          baseMap += (l -> v)
          oldValue.orNull
        }
        case s: String => {
          val l = s.toLong
          val oldValue = baseMap.get(l)
          baseMap += (l -> v)
          oldValue.orNull
        }
        case _ => {
          val typeName = Option(k) match {
            case Some(n) => n.getClass.getName
            case _ => "null"
          }
          throw new IllegalArgumentException(s"LongMap does not support keys of type $typeName")
        }
      }
    }

    // Used by the deserializer when using readerForUpdating
    override def get(key: Object): Object = key match {
      case n: Number => baseMap.get(n.longValue()).orNull
      case s: String => baseMap.get(s.toInt).orNull
      case _ => None.orNull
    }

    // Isn't used by the deserializer
    override def entrySet(): java.util.Set[java.util.Map.Entry[Object, Object]] =
      baseMap.asJava.entrySet().asInstanceOf[java.util.Set[java.util.Map.Entry[Object, Object]]]

    def asLongMap[V](): immutable.LongMap[V] = baseMap.asInstanceOf[immutable.LongMap[V]]
  }

  private class MutableMapWrapper extends util.AbstractMap[Object, Object] {
    var baseMap = mutable.LongMap[Object]()

    override def put(k: Object, v: Object): Object = {
      k match {
        case n: Number => {
          val l = n.longValue()
          val oldValue = baseMap.get(l)
          baseMap += (l -> v)
          oldValue.orNull
        }
        case s: String => {
          val l = s.toLong
          val oldValue = baseMap.get(l)
          baseMap += (l -> v)
          oldValue.orNull
        }
        case _ => {
          val typeName = Option(k) match {
            case Some(n) => n.getClass.getName
            case _ => "null"
          }
          throw new IllegalArgumentException(s"LongMap does not support keys of type $typeName")
        }
      }
    }

    // Used by the deserializer when using readerForUpdating
    override def get(key: Object): Object = key match {
      case n: Number => baseMap.get(n.longValue()).orNull
      case s: String => baseMap.get(s.toInt).orNull
      case _ => None.orNull
    }

    // Isn't used by the deserializer
    override def entrySet(): java.util.Set[java.util.Map.Entry[Object, Object]] =
      baseMap.asJava.entrySet().asInstanceOf[java.util.Set[java.util.Map.Entry[Object, Object]]]

    def asLongMap[V](): mutable.LongMap[V] = baseMap.asInstanceOf[mutable.LongMap[V]]
  }

}
