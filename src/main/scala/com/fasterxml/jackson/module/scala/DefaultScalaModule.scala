package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.module.scala.deser.{
  ScalaNumberDeserializersModule,
  ScalaObjectDeserializerModule,
  UntypedObjectDeserializerModule
}
import com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule

/**
 * Complete module with support for all features, with the exception of [[BitSetDeserializerModule]].
 *
 * This class aggregates all of the feature modules into a single concrete class.
 * Its use is recommended for new users and users who want things to "just work".
 * If more customized support is desired, consult each of the constituent traits.
 *
 * @see [[com.fasterxml.jackson.module.scala.JacksonModule]]
 *
 * @since 1.9.0
 */
class DefaultScalaModule
  extends JacksonModule
     with IteratorModule
     with EnumerationModule
     with OptionModule
     with SeqModule
     with IterableModule
     with TupleModule
     with MapModule
     with SetModule
     with ScalaNumberDeserializersModule
     with ScalaObjectDeserializerModule
     with ScalaAnnotationIntrospectorModule
     with UntypedObjectDeserializerModule
     with EitherModule
     with SymbolModule
{
  override def getModuleName: String = "DefaultScalaModule"
}

object DefaultScalaModule extends DefaultScalaModule
