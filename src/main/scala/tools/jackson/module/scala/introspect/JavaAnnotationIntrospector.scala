package tools.jackson.module.scala.introspect

import tools.jackson.core.Version
import tools.jackson.databind.PropertyName
import tools.jackson.databind.cfg.MapperConfig
import tools.jackson.databind.introspect.{Annotated, AnnotatedMember, AnnotatedParameter, NopAnnotationIntrospector}
import tools.jackson.module.scala.{JacksonModule, ScalaModule}

import java.lang.reflect.{Constructor, Field, Method, Parameter}
import scala.reflect.NameTransformer

object JavaAnnotationIntrospector extends JavaAnnotationIntrospectorInstance(ScalaModule.defaultBuilder)

class JavaAnnotationIntrospectorInstance(config: ScalaModule.Config) extends NopAnnotationIntrospector {

  override def findNameForDeserialization(config: MapperConfig[_], a: Annotated): PropertyName = None.orNull

  override def findImplicitPropertyName(config: MapperConfig[_], param: AnnotatedMember): String = {
    val result = param match {
      case param: AnnotatedParameter if ScalaAnnotationIntrospector.isMaybeScalaBeanType(param.getDeclaringClass) => {
        val index = param.getIndex
        val owner = param.getOwner
        owner.getAnnotated match {
          case ctor: Constructor[_] => {
            val names = JavaParameterIntrospector.getCtorParamNames(ctor)
            if (index < names.length) Option(names(index)) else None
          }
          case method: Method => {
            val names = JavaParameterIntrospector.getMethodParamNames(method)
            if (index < names.length) Option(names(index)) else None
          }
          case field: Field => Option(JavaParameterIntrospector.getFieldName(field))
          case parameter: Parameter => Option(JavaParameterIntrospector.getParameterName(parameter))
          case _ => None
        }
      }
      case _ => None
    }
    result.map(NameTransformer.decode).getOrElse(None.orNull)
  }

  override def version(): Version = JacksonModule.version
}
