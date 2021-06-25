package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.deser.Deserializers
import com.fasterxml.jackson.databind.deser.jdk.UntypedObjectDeserializer
import com.fasterxml.jackson.module.scala.{JacksonModule, ScalaModule}

import scala.languageFeature.postfixOps

private class UntypedScalaObjectDeserializer extends UntypedObjectDeserializer(null, null) {

  private var _mapDeser: ValueDeserializer[AnyRef] = _
  private var _listDeser: ValueDeserializer[AnyRef] = _

  override def resolve(ctxt: DeserializationContext): Unit = {
    super.resolve(ctxt)
    val anyRef = ctxt.constructType(classOf[AnyRef])
    val string = ctxt.constructType(classOf[String])
    val tf = ctxt.getTypeFactory
    _mapDeser = ctxt.findRootValueDeserializer(
      ctxt.getConfig.mapAbstractType(
        tf.constructMapLikeType(classOf[collection.Map[_,_]], string, anyRef))).asInstanceOf[ValueDeserializer[AnyRef]]
    _listDeser = ctxt.findRootValueDeserializer(
      ctxt.getConfig.mapAbstractType(
        tf.constructCollectionLikeType(classOf[collection.Seq[_]], anyRef))).asInstanceOf[ValueDeserializer[AnyRef]]
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


private class UntypedObjectDeserializerResolver(builder: ScalaModule.ReadOnlyBuilder) extends Deserializers.Base {

  private val objectClass = classOf[AnyRef]

  override def findBeanDeserializer(javaType: JavaType,
                                    config: DeserializationConfig,
                                    beanDesc: BeanDescription) =
    if (!objectClass.equals(javaType.getRawClass)) null
    else new UntypedScalaObjectDeserializer

  override def hasDeserializerFor(config: DeserializationConfig, valueType: Class[_]): Boolean = {
    objectClass == valueType
  }
}

trait UntypedObjectDeserializerModule extends JacksonModule {
  this += (_ addDeserializers new UntypedObjectDeserializerResolver(builder))
}

object UntypedObjectDeserializerModule extends UntypedObjectDeserializerModule
