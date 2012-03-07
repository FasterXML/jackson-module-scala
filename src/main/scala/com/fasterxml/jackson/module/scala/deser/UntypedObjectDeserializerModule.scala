package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.module.scala.JacksonModule
import org.codehaus.jackson.map.deser.std.{UntypedObjectDeserializer => JacksonUntypedObjectDeserializer}
import org.codehaus.jackson.JsonParser
import scala.collection.JavaConverters._
import java.util.{LinkedHashMap, ArrayList}
import org.codehaus.jackson.`type`.JavaType
import org.codehaus.jackson.map.{BeanProperty, DeserializerProvider, BeanDescription, DeserializationConfig, DeserializationContext, Deserializers}

private class UntypedObjectDeserializer extends JacksonUntypedObjectDeserializer {

  override def mapArray(jp: JsonParser, ctxt: DeserializationContext): AnyRef = {
    if (ctxt.isEnabled(DeserializationConfig.Feature.USE_JAVA_ARRAY_FOR_JSON_ARRAY)) {
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
                                    provider: DeserializerProvider,
                                    beanDesc: BeanDescription,
                                    property: BeanProperty) =
    if (!OBJECT.equals(javaType.getRawClass)) null
    else new UntypedObjectDeserializer
}

trait UntypedObjectDeserializerModule extends JacksonModule {
  this += (_ addDeserializers UntypedObjectDeserializerResolver)
}
