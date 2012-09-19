package com.fasterxml.jackson.module.scala.ser

import collection.JavaConverters._

import java.{util => ju}


import com.fasterxml.jackson.databind.{BeanDescription, SerializationConfig}
import com.fasterxml.jackson.databind.ser.{BeanPropertyWriter, BeanSerializerModifier}
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod
import com.fasterxml.jackson.databind.util.SimpleBeanPropertyDefinition
import com.fasterxml.jackson.module.scala.JacksonModule
import com.fasterxml.jackson.module.scala.reflect.BeanMirror

private object CaseClassBeanSerializerModifier extends BeanSerializerModifier {
  private val PRODUCT = classOf[Product]

  override def changeProperties(config: SerializationConfig,
                                beanDesc: BeanDescription,
                                beanProperties: ju.List[BeanPropertyWriter]): ju.List[BeanPropertyWriter] = {
    val jacksonIntrospector = config.getAnnotationIntrospector
    val list = try {
      BeanMirror(beanDesc.getBeanClass).readableProperties.values.map{ property =>
        val method = beanDesc.findMethod(property.name, Array())
        property.constructorParameterIndex match {
        case Some(paramIndex) =>
          val param = beanDesc.getConstructors.get(0).getParameter(paramIndex)
          asWriter(config, beanDesc, method, Option(jacksonIntrospector.findDeserializationName(param)))
        case None => asWriter(config, beanDesc, method)
      }}
    } catch {
      case _: IllegalArgumentException => Nil
    }

    if (list.isEmpty) beanProperties else new ju.ArrayList[BeanPropertyWriter](list.toList.asJava)
  }

  private def asWriter(config: SerializationConfig, beanDesc: BeanDescription, member: AnnotatedMethod, primaryName: Option[String] = None) = {
    val javaType = config.getTypeFactory.constructType(member.getGenericType)
    val name = primaryName.getOrElse(member.getName)
    val propDef = new SimpleBeanPropertyDefinition(member, name)
    new BeanPropertyWriter(propDef, member, null, javaType, null, null, null, false, null)
  }

}

trait CaseClassSerializerModule extends JacksonModule {
  this += { _.addBeanSerializerModifier(CaseClassBeanSerializerModifier) }
}