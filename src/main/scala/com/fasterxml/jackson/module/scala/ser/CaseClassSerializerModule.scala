package com.fasterxml.jackson.module.scala.ser

import collection.JavaConverters._

import java.{util => ju}

import com.fasterxml.jackson.annotation.{JsonInclude, JsonIgnore}
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import com.fasterxml.jackson.databind.{PropertyName, BeanDescription, SerializationConfig}
import com.fasterxml.jackson.databind.ser.{BeanSerializerFactory, BeanPropertyWriter, BeanSerializerModifier}
import com.fasterxml.jackson.databind.introspect.{AnnotatedField, AnnotatedMember, AnnotatedMethod}
import com.fasterxml.jackson.databind.util.SimpleBeanPropertyDefinition
import com.fasterxml.jackson.module.scala.JacksonModule
import reflect.NameTransformer
import com.fasterxml.jackson.module.scala.introspect.{PropertyDescriptor, BeanIntrospector}

private object CaseClassBeanSerializerModifier extends BeanSerializerModifier {
  private val PRODUCT = classOf[Product]

  override def changeProperties(config: SerializationConfig,
                                beanDesc: BeanDescription,
                                beanProperties: ju.List[BeanPropertyWriter]): ju.List[BeanPropertyWriter] = {
    val jacksonIntrospector = config.getAnnotationIntrospector
    val defaultInclusion = beanDesc.findSerializationInclusion(config.getSerializationInclusion)

    val list = for {
      cls <- Option(beanDesc.getBeanClass).toSeq if (PRODUCT.isAssignableFrom(cls))
      prop <- BeanIntrospector(cls).properties
      if (prop.findAnnotation[JsonIgnore].map(!_.value).getOrElse(true))
      // Check for the JsonInclude annotation
      suppressNulls = prop.findAnnotation[JsonInclude].getOrElse(defaultInclusion) == NON_NULL
      getter <- prop.getter
      method <- beanDesc.getClassInfo.memberMethods().asScala.find(_.getAnnotated equals getter)
    } yield {
      prop.param match {
        case Some(p) =>
          val param = beanDesc.getConstructors.get(0).getParameter(p.index)
          asWriter(config, beanDesc, method, suppressNulls, Option(jacksonIntrospector.findNameForDeserialization(param)))
        case _ => asWriter(config, beanDesc, method, suppressNulls)
      }
    }

    if (list.isEmpty) beanProperties else new ju.ArrayList[BeanPropertyWriter](list.toList.asJava)
  }

  private def asWriter(config: SerializationConfig, beanDesc: BeanDescription, member: AnnotatedMethod, suppressNulls: Boolean, primaryName: Option[PropertyName] = None) = {
    val javaType = config.getTypeFactory.constructType(member.getGenericType)
    val defaultName = NameTransformer.decode(member.getName)
    val name = maybeTranslateName(config, member, primaryName.map(_.toString).getOrElse(defaultName))
    val propDef = new SimpleBeanPropertyDefinition(member, name)

    val jsf = BeanSerializerFactory.instance
    val typeSer = jsf.findPropertyTypeSerializer(javaType, config, member)

    new BeanPropertyWriter(propDef, member, null, javaType, null, typeSer, null, suppressNulls, null)
  }

  private def maybeTranslateName(config: SerializationConfig, member: AnnotatedMember, name: String) = {
    Option(config.getPropertyNamingStrategy).map { ns =>
      member match {
        case f: AnnotatedField => ns.nameForField(config, f, name)
        case m: AnnotatedMethod => ns.nameForGetterMethod(config, m, name)
      }
    } getOrElse name
  }

}

trait CaseClassSerializerModule extends JacksonModule {
  this += { _.addBeanSerializerModifier(CaseClassBeanSerializerModifier) }
}