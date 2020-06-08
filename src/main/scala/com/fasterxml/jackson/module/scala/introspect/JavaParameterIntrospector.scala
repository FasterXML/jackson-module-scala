package com.fasterxml.jackson.module.scala.introspect

import java.lang.reflect.Constructor

private[introspect] object JavaParameterIntrospector {

  def getCtorParams(ctor: Constructor[_]): Seq[String] =
    ctor.getParameters.map(_.getName).toSeq
}
