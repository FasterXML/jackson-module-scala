package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.`type`.MapLikeType
import com.fasterxml.jackson.databind.deser.{ContextualDeserializer, Deserializers}
import com.fasterxml.jackson.databind.deser.std.{ContainerDeserializerBase, MapDeserializer, StdValueInstantiator}
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.module.scala.{DefaultScalaModule, IteratorModule, JacksonModule}

import java.util
import scala.collection.immutable
import scala.collection.immutable.IntMap
import scala.languageFeature.postfixOps

/**
 * Adds support for deserializing Scala [[scala.collection.immutable.IntMap]]s. Scala IntMaps can already be
 * serialized using [[IteratorModule]] or [[DefaultScalaModule]].
 *
 * @since 2.14.0
 */
private object ImmutableIntMapDeserializerResolver extends Deserializers.Base {
  override def findMapLikeDeserializer(theType: MapLikeType,
                                       config: DeserializationConfig,
                                       beanDesc: BeanDescription,
                                       keyDeserializer: KeyDeserializer,
                                       elementTypeDeserializer: TypeDeserializer,
                                       elementDeserializer: JsonDeserializer[_]): JsonDeserializer[_] = {
    if (!classOf[immutable.IntMap[_]].isAssignableFrom(theType.getRawClass)) None.orNull
    else {
      val mapDeserialiser = new MapDeserializer(theType, new IntMapInstantiator(config, theType), keyDeserializer,
        elementDeserializer.asInstanceOf[JsonDeserializer[AnyRef]], elementTypeDeserializer)
      new IntMapDeserializer(theType, mapDeserialiser)
    }
  }

  private class IntMapDeserializer[V](mapType: MapLikeType, containerDeserializer: MapDeserializer)
    extends ContainerDeserializerBase[IntMap[V]](mapType) with ContextualDeserializer {

    override def getContentType: JavaType = containerDeserializer.getContentType

    override def getContentDeserializer: JsonDeserializer[AnyRef] = containerDeserializer.getContentDeserializer

    override def createContextual(ctxt: DeserializationContext, property: BeanProperty): JsonDeserializer[_] = {
      val newDelegate = containerDeserializer.createContextual(ctxt, property).asInstanceOf[MapDeserializer]
      new IntMapDeserializer(mapType, newDelegate)
    }

    override def deserialize(jp: JsonParser, ctxt: DeserializationContext): IntMap[V] = {
      containerDeserializer.deserialize(jp, ctxt) match {
        case wrapper: BuilderWrapper => wrapper.asIntMap()
      }
    }

    override def deserialize(jp: JsonParser, ctxt: DeserializationContext, intoValue: IntMap[V]): IntMap[V] = {
      val newMap = deserialize(jp, ctxt)
      if (newMap.isEmpty) {
        intoValue
      } else {
        intoValue ++ newMap
      }
    }

    override def getEmptyValue(ctxt: DeserializationContext): Object = IntMap.empty[V]
  }

  private class IntMapInstantiator(config: DeserializationConfig, mapType: MapLikeType) extends StdValueInstantiator(config, mapType) {
    override def canCreateUsingDefault = true
    override def createUsingDefault(ctxt: DeserializationContext) = new BuilderWrapper
  }

  private class BuilderWrapper extends util.AbstractMap[Any, Any] {
    var intMap = IntMap[Any]()

    override def put(k: Any, v: Any): Any = {
      k match {
        case i: Int => intMap += (i -> v)
        case n: Number => intMap += (n.intValue() -> v)
        case s: String => intMap += (s.toInt -> v)
        case _ => {
          val typeName = Option(k) match {
            case Some(n) => n.getClass.getName
            case _ => "null"
          }
          throw new IllegalArgumentException(s"IntMap does npt support keys of type $typeName")
        }
      }
      v
    }

    // Used by the deserializer when using readerForUpdating
    override def get(key: Any): Any = key match {
      case i: Int => intMap.get(i).orNull
      case _ => None.orNull
    }

    // Isn't used by the deserializer
    override def entrySet(): java.util.Set[java.util.Map.Entry[Any, Any]] = throw new UnsupportedOperationException

    def asIntMap[V](): IntMap[V] = intMap.asInstanceOf[IntMap[V]]
  }
}

trait IntMapDeserializerModule extends JacksonModule {
  this += ImmutableIntMapDeserializerResolver
}
