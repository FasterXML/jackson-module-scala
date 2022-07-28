package tools.jackson.module.scala.introspect

case class BeanDescriptor(beanType: Class[_], properties: Seq[PropertyDescriptor])
