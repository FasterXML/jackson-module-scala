package com.fasterxml.jackson.module.scala.deser

import scala.collection.{GenMap, mutable}
import java.util.AbstractMap
import java.util.Map.Entry
import scala.collection.immutable.HashMap
import org.codehaus.jackson.`type`.JavaType
import org.codehaus.jackson.map.deser.std.{MapDeserializer, ContainerDeserializerBase}
import org.codehaus.jackson.JsonParser
import org.codehaus.jackson.map.`type`.MapLikeType
import org.codehaus.jackson.map.{BeanProperty, BeanDescription, DeserializerProvider, DeserializationContext, TypeDeserializer, JsonDeserializer, KeyDeserializer, DeserializationConfig, Deserializers}
import org.codehaus.jackson.map.deser.ValueInstantiator
import com.fasterxml.jackson.module.scala.modifiers.MapTypeModifierModule

private class MapBuilderWrapper[K,V](val builder: mutable.Builder[(K,V), GenMap[K,V]]) extends AbstractMap[K,V] {
  override def put(k: K, v: V) = { builder += ((k,v)); v }

  // Isn't used by the deserializer
  def entrySet(): java.util.Set[Entry[K, V]] = throw new UnsupportedOperationException
}

private object UnsortedMapDeserializer {
  def builderFor(cls: Class[_]): mutable.Builder[(AnyRef,AnyRef), GenMap[AnyRef,AnyRef]] =
    if (classOf[HashMap[_,_]].isAssignableFrom(cls)) HashMap.newBuilder[AnyRef,AnyRef] else
    if (classOf[mutable.HashMap[_,_]].isAssignableFrom(cls)) mutable.HashMap.newBuilder[AnyRef,AnyRef] else
    if (classOf[mutable.LinkedHashMap[_,_]].isAssignableFrom(cls)) mutable.LinkedHashMap.newBuilder[AnyRef,AnyRef] else
    Map.newBuilder[AnyRef,AnyRef]
}

private [deser] class UnsortedMapDeserializer(
    collectionType: MapLikeType,
    config: DeserializationConfig,
    keyDeser: KeyDeserializer,
    valueDeser: JsonDeserializer[_],
    valueTypeDeser: TypeDeserializer)

  extends ContainerDeserializerBase[GenMap[AnyRef,AnyRef]](classOf[UnsortedMapDeserializer]) {

  private val javaContainerType = config.constructType(classOf[MapBuilderWrapper[AnyRef,AnyRef]])

  private val instantiator =
    new ValueInstantiator {
      def getValueTypeDesc = collectionType.getRawClass.getCanonicalName
      override def canCreateUsingDefault = true
      override def createUsingDefault =
        new MapBuilderWrapper[AnyRef,AnyRef](UnsortedMapDeserializer.builderFor(collectionType.getRawClass))
    }

  private val containerDeserializer =
    new MapDeserializer(javaContainerType,instantiator,keyDeser,valueDeser.asInstanceOf[JsonDeserializer[AnyRef]],valueTypeDeser)

  override def getContentType = containerDeserializer.getContentType

  override def getContentDeserializer = containerDeserializer.getContentDeserializer

  override def deserialize(jp: JsonParser, ctxt: DeserializationContext): GenMap[AnyRef,AnyRef] = {
//    val builder = UnsortedMapDeserializer.builderFor(collectionType.getRawClass)
//    containerDeserializer.deserialize(jp, ctxt, new MapBuilderWrapper[AnyRef,AnyRef](builder))
//    builder.result()
    containerDeserializer.deserialize(jp,ctxt) match {
      case wrapper: MapBuilderWrapper[AnyRef, AnyRef] => wrapper.builder.result()
    }
  }
}

object UnsortedMapDeserializerResolver extends Deserializers.Base {

  override def findMapLikeDeserializer(theType: MapLikeType,
                              config: DeserializationConfig,
                              provider: DeserializerProvider,
                              beanDesc: BeanDescription,
                              property: BeanProperty,
                              keyDeserializer: KeyDeserializer,
                              elementTypeDeserializer: TypeDeserializer,
                              elementDeserializer: JsonDeserializer[_]): JsonDeserializer[_] = {
    val rawClass = theType.getRawClass
    if (classOf[collection.Map[_,_]].isAssignableFrom(rawClass) &&
        !classOf[collection.SortedMap[_,_]].isAssignableFrom(rawClass)) {
      val keyType = theType.containedType(0)
      val valueType = theType.containedType(1)
      val resolvedKeyDeser =
        Option(keyDeserializer).getOrElse(provider.findKeyDeserializer(config,keyType,property))
      val resolvedValueDeser =
        Option(elementDeserializer).getOrElse(provider.findValueDeserializer(config,valueType,property))
      new UnsortedMapDeserializer(theType,config,resolvedKeyDeser,resolvedValueDeser,elementTypeDeserializer)
    } else null
  }

}

/**
 * @author Christopher Currie <ccurrie@impresys.com>
 */
trait UnsortedMapDeserializerModule extends MapTypeModifierModule {
  this += UnsortedMapDeserializerResolver
}