package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.databind.`type`.TypeBindings
import com.fasterxml.jackson.databind.deser.std.StdValueInstantiator
import com.fasterxml.jackson.databind.deser.{CreatorProperty, ValueInstantiator, ValueInstantiators}
import com.fasterxml.jackson.databind.{BeanDescription, DeserializationConfig}
import com.fasterxml.jackson.module.scala.JacksonModule
import scala.collection.JavaConverters._
import scala.None
import com.fasterxml.jackson.module.scala.util.Implicts._

private class ScalaValueInstantiator(config: DeserializationConfig, beanDesc: BeanDescription)
  extends StdValueInstantiator(config, beanDesc.getType) {

  private [this] lazy val _typeBindings = new TypeBindings(config.getTypeFactory, beanDesc.getType)
  private [this] lazy val _ctorProps = for {
    prop <- beanDesc.findProperties().asScala
    param <- Option(prop.getConstructorParameter)
    name = prop.getName
    wrap = prop.getWrapperName
    idx = param.getIndex
    typ = param.getType(_typeBindings)
  } yield {
    new CreatorProperty(name, typ, wrap, null, null, param, idx, null, true)
  }

  val creator = beanDesc.getConstructors.asScala.headOption
  val defaultCtor = if (creator.isDefined) None else Option(beanDesc.findDefaultConstructor)

  configureFromObjectSettings(defaultCtor.orNull, null, null, null, creator.orNull, creator.map(_ => _ctorProps.toArray).orNull)
}

private object ScalaValueInstantiators extends ValueInstantiators.Base {
  override def findValueInstantiator(config: DeserializationConfig,
                                     beanDesc: BeanDescription,
                                     defaultInstantiator: ValueInstantiator) = {
    if (beanDesc.getBeanClass.hasSignature)
      new ScalaValueInstantiator(config, beanDesc)
    else
      defaultInstantiator
  }

}

trait ScalaValueInstantiatorsModule extends JacksonModule {
  this += { _.addValueInstantiators(ScalaValueInstantiators) }
}