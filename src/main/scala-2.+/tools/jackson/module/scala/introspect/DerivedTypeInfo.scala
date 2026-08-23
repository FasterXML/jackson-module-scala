package tools.jackson.module.scala.introspect

/**
 * Deriving what the JVM erases is a Scala 3 way of capturing it, so there is nothing to read here.
 * Scala 2 users register the same thing with `registerReferencedValueType`.
 */
private[introspect] object DerivedTypeInfo {
  def erasedTypeArguments(clazz: Class[_]): Seq[(String, Class[_])] = Seq.empty
}
