package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.module.scala.deser.{ScalaStdValueInstantiatorsModule, UntypedObjectDeserializerModule}
import com.fasterxml.jackson.module.scala.introspect.ScalaClassIntrospectorModule
import com.fasterxml.jackson.module.scala.modifiers.EitherModule

/**
 * Complete module with support for all features.
 *
 * This class aggregates all of the feature modules into a single concrete class.
 * Its use is recommended for new users and users who want things to "just work".
 * If more customized support is desired, consult each of the constituent traits.
 *
 * @see [[com.fasterxml.jackson.module.scala.JacksonModule]]
 *
 * @author Christopher Currie <christopher@currie.com>
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
     with ScalaStdValueInstantiatorsModule
     with ScalaClassIntrospectorModule
     with UntypedObjectDeserializerModule
     with EitherModule
{
  override def getModuleName = "DefaultScalaModule"
}

object DefaultScalaModule extends DefaultScalaModule
