package com.fasterxml.jackson.module.scala
package introspect

import java.lang.reflect.{Constructor, Field, Method}

case class ConstructorParameter(constructor: Constructor[_], index: Int, defaultValue: Option[() => AnyRef])

case class PropertyDescriptor(name: String,
                              param: Option[ConstructorParameter],
                              field: Option[Field],
                              getter: Option[Method],
                              setter: Option[Method],
                              beanGetter: Option[Method],
                              beanSetter: Option[Method]) {
  if (List(field, getter).flatten.isEmpty) throw new IllegalArgumentException("One of field or getter must be defined.")
}
