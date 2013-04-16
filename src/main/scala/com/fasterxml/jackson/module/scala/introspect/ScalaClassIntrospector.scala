package com.fasterxml.jackson.module.scala.introspect

import com.fasterxml.jackson.databind.introspect.{AnnotatedClass, BasicClassIntrospector}
import com.fasterxml.jackson.databind.cfg.MapperConfig
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.module.scala.JacksonModule
import com.fasterxml.jackson.module.scala.util.Implicts._

object ScalaClassIntrospector extends BasicClassIntrospector {

  private def isScalaPackage(pkg: Option[Package]): Boolean =
    pkg flatMap { _.getName.split("\\.").headOption } map { _ == "scala" } getOrElse false

  private def isMaybeScalaBeanType(cls: Class[_]): Boolean =
    cls.hasSignature && !isScalaPackage(Option(cls.getPackage))

  protected override def constructPropertyCollector(config: MapperConfig[_],
                                                    ac: AnnotatedClass,
                                                    `type`: JavaType,
                                                    forSerialization: Boolean,
                                                    mutatorPrefix: String) = {
    val erased = `type`.getRawClass
    if (isMaybeScalaBeanType(erased))
      new ScalaPropertiesCollector(config, forSerialization, `type`, ac, mutatorPrefix)
    else
      super.constructPropertyCollector(config, ac, `type`, forSerialization, mutatorPrefix)
  }

}

trait ScalaClassIntrospectorModule extends JacksonModule {
  this += { _.setClassIntrospector(ScalaClassIntrospector) }
}