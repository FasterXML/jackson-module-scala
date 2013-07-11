package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.{DeserializationFeature, JavaType, BeanDescription, DeserializationConfig, DeserializationContext}
import com.fasterxml.jackson.databind.deser.Deserializers
import com.fasterxml.jackson.databind.deser.std
import com.fasterxml.jackson.module.scala.JacksonModule
import com.fasterxml.jackson.module.scala.util.Implicts._
import java.{util => ju}
import scala.collection.JavaConverters._
import com.fasterxml.jackson.core.`type`.TypeReference

object UntypedObjectDeserializer
{
  lazy val SEQ = new TypeReference[collection.Seq[Any]] {}
  lazy val MAP = new TypeReference[collection.Map[String,Any]] {}
}

private class UntypedObjectDeserializer extends std.UntypedObjectDeserializer {
  import UntypedObjectDeserializer._

  override def mapArray(jp: JsonParser, ctxt: DeserializationContext): AnyRef = {
    if (ctxt.isEnabled(DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY)) {
      mapArrayToArray(jp, ctxt)
    }
    else {
      val factory = ctxt.getTypeFactory
      val seqType = factory.constructType(SEQ)
      deserializeAbstractType(jp, ctxt, seqType) getOrElse {
        super.mapArray(jp, ctxt).asInstanceOf[ju.List[AnyRef]].asScala
      }
    }
  }

  override def mapObject(jp: JsonParser, ctxt: DeserializationContext): AnyRef = {
    val factory = ctxt.getTypeFactory
    val mapType = factory.constructType(MAP)
    deserializeAbstractType(jp, ctxt, mapType) getOrElse {
      super.mapObject(jp, ctxt).asInstanceOf[ju.Map[String, AnyRef]].asScala
    }
  }

  private def deserializeAbstractType(jp: JsonParser, ctxt: DeserializationContext, javaType: JavaType): Option[AnyRef] = {
    val concreteType = ctxt.getFactory.mapAbstractType(ctxt.getConfig, javaType)
    Option(concreteType) optMap { ctxt.findRootValueDeserializer } map { _.deserialize(jp, ctxt) }
  }
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
