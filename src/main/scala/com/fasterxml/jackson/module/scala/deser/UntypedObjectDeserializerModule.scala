package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.deser.{Deserializers, std}
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.module.scala.JacksonModule

private class UntypedScalaObjectDeserializer extends std.UntypedObjectDeserializer(null, null) {

  private var _mapDeser: JsonDeserializer[AnyRef] = _
  private var _listDeser: JsonDeserializer[AnyRef] = _

  override def resolve(ctxt: DeserializationContext): Unit = {
    super.resolve(ctxt)
    val anyRef = ctxt.constructType(classOf[AnyRef])
    val string = ctxt.constructType(classOf[String])
    val factory = ctxt.getFactory
    val tf = ctxt.getTypeFactory
    _mapDeser = ctxt.findRootValueDeserializer(
      factory.mapAbstractType(ctxt.getConfig, tf.constructMapLikeType(classOf[collection.Map[_,_]], string, anyRef)))
    _listDeser = ctxt.findRootValueDeserializer(
      factory.mapAbstractType(ctxt.getConfig, tf.constructCollectionLikeType(classOf[collection.Seq[_]], anyRef)))
  }

  override def mapArray(jp: JsonParser, ctxt: DeserializationContext): AnyRef = {
    if (ctxt.isEnabled(DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY)) {
      mapArrayToArray(jp, ctxt)
    }
    else {
      _listDeser.deserialize(jp, ctxt)
    }
  }

  override def mapObject(jp: JsonParser, ctxt: DeserializationContext): AnyRef = {
    _mapDeser.deserialize(jp, ctxt)
  }
}


private object UntypedObjectDeserializerResolver extends Deserializers.Base {

  lazy val OBJECT = classOf[AnyRef]

  override def findBeanDeserializer(javaType: JavaType,
                                    config: DeserializationConfig,
                                    beanDesc: BeanDescription) =
    if (!OBJECT.equals(javaType.getRawClass)) None.orNull
    else new UntypedScalaObjectDeserializer
}

trait UntypedObjectDeserializerModule extends JacksonModule {
  this += (_ addDeserializers UntypedObjectDeserializerResolver)
}
