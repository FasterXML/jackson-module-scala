package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.module.scala.JacksonModule
import com.fasterxml.jackson.databind.deser.std.{UntypedObjectDeserializer => JacksonUntypedObjectDeserializer}
import com.fasterxml.jackson.core.JsonParser
import scala.collection.JavaConverters._
import java.util.{LinkedHashMap, ArrayList}
import com.fasterxml.jackson.databind.deser.Deserializers
import com.fasterxml.jackson.databind.{DeserializationFeature, JavaType, BeanDescription, DeserializationConfig, DeserializationContext}

private class UntypedObjectDeserializer extends JacksonUntypedObjectDeserializer {

  override def mapArray(jp: JsonParser, ctxt: DeserializationContext): AnyRef = {
    if (ctxt.isEnabled(DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY)) {
      mapArrayToArray(jp, ctxt)
    }
    else {
      super.mapArray(jp, ctxt).asInstanceOf[ArrayList[AnyRef]].asScala
    }
  }

  override def mapObject(jp: JsonParser, ctxt: DeserializationContext): AnyRef =
    super.mapObject(jp, ctxt).asInstanceOf[LinkedHashMap[String, AnyRef]].asScala

}


private object UntypedObjectDeserializerResolver extends Deserializers.Base {

  lazy val OBJECT = classOf[AnyRef]
  
  override def findBeanDeserializer(javaType: JavaType,
                                    config: DeserializationConfig,
                                    beanDesc: BeanDescription) =
    if (!OBJECT.equals(javaType.getRawClass)) null
    else new UntypedObjectDeserializer
}

trait UntypedObjectDeserializerModule extends JacksonModule {
  this += (_ addDeserializers UntypedObjectDeserializerResolver)
}
