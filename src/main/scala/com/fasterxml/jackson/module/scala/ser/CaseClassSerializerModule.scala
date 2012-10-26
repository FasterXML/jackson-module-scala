package com.fasterxml.jackson.module.scala.ser

import collection.JavaConverters._

import java.{util => ju}

import org.scalastuff.scalabeans.ConstructorParameter

import com.fasterxml.jackson.annotation.{JsonInclude, JsonIgnore}
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import com.fasterxml.jackson.databind.{BeanDescription, SerializationConfig}
import com.fasterxml.jackson.databind.ser.{BeanPropertyWriter, BeanSerializerModifier}
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod
import com.fasterxml.jackson.databind.util.SimpleBeanPropertyDefinition
import com.fasterxml.jackson.module.scala.JacksonModule
import com.fasterxml.jackson.module.scala.util.ScalaBeansUtil

private object CaseClassBeanSerializerModifier extends BeanSerializerModifier {
  private val PRODUCT = classOf[Product]

  override def changeProperties(config: SerializationConfig,
                                beanDesc: BeanDescription,
                                beanProperties: ju.List[BeanPropertyWriter]): ju.List[BeanPropertyWriter] = {
    val jacksonIntrospector = config.getAnnotationIntrospector
    val defaultInclusion = beanDesc.findSerializationInclusion(config.getSerializationInclusion)

    val list = for {
      cls <- Option(beanDesc.getBeanClass).toSeq if (PRODUCT.isAssignableFrom(cls))
      prop <- ScalaBeansUtil.propertiesOf(cls) if (prop.findAnnotation[JsonIgnore].map(!_.value).getOrElse(true))
      // Not completely happy with this test. I'd rather check the PropertyDescription
      // to see if it's a field or a method, but ScalaBeans doesn't expose that as yet.
      // I'm not sure if it truly matters as Scala generates method accessors for fields.
      // This is also realy inefficient, as we're doing a find on each iteration of the loop.
      method <- Option(beanDesc.findMethod(prop.name, Array()))
    } yield {
      // Check for the JsonInclude annotation
      val suppressNulls = prop.findAnnotation[JsonInclude].getOrElse(defaultInclusion) == NON_NULL
      prop match {
        case cp: ConstructorParameter =>
          val param = beanDesc.getConstructors.get(0).getParameter(cp.index)
          asWriter(config, beanDesc, method, suppressNulls, Option(jacksonIntrospector.findDeserializationName(param)))
        case _ => asWriter(config, beanDesc, method, suppressNulls)
      }
    }

    if (list.isEmpty) beanProperties else new ju.ArrayList[BeanPropertyWriter](list.toList.asJava)
  }

  private def asWriter(config: SerializationConfig, beanDesc: BeanDescription, member: AnnotatedMethod, suppressNulls: Boolean, primaryName: Option[String] = None) = {
    val javaType = config.getTypeFactory.constructType(member.getGenericType)
    val name = maybeTranslateName(config, member, primaryName.getOrElse(member.getName))
    val propDef = new SimpleBeanPropertyDefinition(member, scala.reflect.NameTransformer.decode(name))
    new BeanPropertyWriter(propDef, member, null, javaType, null, null, null, suppressNulls, null)
  }

  private def maybeTranslateName(config: SerializationConfig, member: AnnotatedMethod, name: String) = {
    Option(config.getPropertyNamingStrategy).map(_.nameForGetterMethod(config, member, name)).getOrElse(name)
  }

}

trait CaseClassSerializerModule extends JacksonModule {
  this += { _.addBeanSerializerModifier(CaseClassBeanSerializerModifier) }
}