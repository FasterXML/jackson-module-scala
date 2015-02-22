package com.fasterxml.jackson.module.scala.introspect

import scala.language.existentials

case class BeanDescriptor(beanType: Class[_], properties: Seq[PropertyDescriptor])