package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.module.scala.deser.{ScalaValueInstantiatorsModule, UntypedObjectDeserializerModule}
import com.fasterxml.jackson.module.scala.introspect.ScalaClassIntrospectorModule

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
sealed class DefaultScalaModule
  extends JacksonModule
     with IteratorModule
     with EnumerationModule
     with OptionModule
     with SeqModule
     with IterableModule
     with TupleModule
     with MapModule
     with SetModule
     with ScalaClassIntrospectorModule
     with UntypedObjectDeserializerModule
{
  override def getModuleName = "DefaultScalaModule"
}

object DefaultScalaModule extends DefaultScalaModule
