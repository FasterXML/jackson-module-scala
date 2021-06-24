package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.module.scala.deser.{ScalaNumberDeserializersModule, UntypedObjectDeserializerModule}
import com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModuleInstance

object ScalaModule {

  trait ReadOnlyBuilder {
    def shouldApplyDefaultValuesWhenDeserializing(): Boolean
  }

  class Builder extends ReadOnlyBuilder {
    private var applyDefaultValuesWhenDeserializing = true

    def applyDefaultValuesWhenDeserializing(applyDefaultValues: Boolean): Builder = {
      applyDefaultValuesWhenDeserializing = applyDefaultValues
      this
    }

    override def shouldApplyDefaultValuesWhenDeserializing(): Boolean = applyDefaultValuesWhenDeserializing

    def build(): JacksonModule = {
      val builderInstance = this
      new ScalaAnnotationIntrospectorModuleInstance(builderInstance)
        with IteratorModule
        with EnumerationModule
        with OptionModule
        with SeqModule
        with IterableModule
        with TupleModule
        with MapModule
        with SetModule
        with ScalaNumberDeserializersModule
        with UntypedObjectDeserializerModule
        with EitherModule
        with SymbolModule
    }
  }

  def builder(): Builder = new Builder()

  val defaultBuilder: ReadOnlyBuilder = builder()
}

