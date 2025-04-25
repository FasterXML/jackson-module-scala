package tools.jackson.module.scala.deser

import tools.jackson.core.JsonParser
import tools.jackson.databind.JacksonModule.SetupContext
import tools.jackson.databind._
import tools.jackson.databind.deser.Deserializers
import tools.jackson.databind.deser.jdk.UntypedObjectDeserializer
import tools.jackson.module.scala.JacksonModule.InitializerBuilder
import tools.jackson.module.scala.{JacksonModule, ScalaModule}

import scala.languageFeature.postfixOps

private class UntypedScalaObjectDeserializer
  extends UntypedObjectDeserializer(null, null.asInstanceOf[JavaType]) {

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


private class UntypedObjectDeserializerResolver(config: ScalaModule.Config) extends Deserializers.Base {

  private val objectClass = classOf[AnyRef]

  override def findBeanDeserializer(javaType: JavaType,
                                    deserializationConfig: DeserializationConfig,
                                    beanDesc: BeanDescription.Supplier) =
    if (!objectClass.equals(javaType.getRawClass)) null
    else new UntypedScalaObjectDeserializer

  override def hasDeserializerFor(deserializationConfig: DeserializationConfig, valueType: Class[_]): Boolean = {
    objectClass == valueType
  }
}

trait UntypedObjectDeserializerModule extends JacksonModule {
  override def getModuleName: String = "UntypedObjectDeserializerModule"

  override def getInitializers(config: ScalaModule.Config): Seq[SetupContext => Unit] = {
    val builder = new InitializerBuilder()
    builder += new UntypedObjectDeserializerResolver(config)
    builder.build()
  }
}

object UntypedObjectDeserializerModule extends UntypedObjectDeserializerModule
