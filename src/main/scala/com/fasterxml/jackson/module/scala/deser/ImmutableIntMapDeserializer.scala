package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.`type`.MapLikeType
import com.fasterxml.jackson.databind.deser.Deserializers
import com.fasterxml.jackson.databind.deser.std.{JsonNodeDeserializer, MapDeserializer, StdDeserializer, StdValueInstantiator}
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.module.scala.{DefaultScalaModule, IteratorModule, JacksonModule}

import java.util
import java.util.Map
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
      new MapDeserializer(theType, new IntMapInstantiator(config, theType), keyDeserializer,
        elementDeserializer.asInstanceOf[JsonDeserializer[AnyRef]], elementTypeDeserializer)
    }
  }

  private class IntMapInstantiator(config: DeserializationConfig, mapType: MapLikeType) extends StdValueInstantiator(config, mapType) {
    var intMap = IntMap[Any]()

    override def canCreateUsingDefault = true

    override def createUsingDefault(ctxt: DeserializationContext) = new util.AbstractMap[Any, Any] {
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
    }

    def asIntMap(): IntMap[_] = intMap
  }
}

trait IntMapDeserializerModule extends JacksonModule {
  this += ImmutableIntMapDeserializerResolver
}
