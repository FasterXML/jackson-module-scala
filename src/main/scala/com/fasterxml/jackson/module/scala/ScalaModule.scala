package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.databind.JacksonModule.SetupContext
import com.fasterxml.jackson.module.scala.deser.{ScalaNumberDeserializersModule, UntypedObjectDeserializerModule}
import com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModuleInstance

object ScalaModule {

  trait ReadOnlyBuilder {
    def shouldApplyDefaultValuesWhenDeserializing(): Boolean
  }

  class Builder extends ReadOnlyBuilder {
    private val initializers = Seq.newBuilder[SetupContext => Unit]
    private var applyDefaultValuesWhenDeserializing = true

    def applyDefaultValuesWhenDeserializing(applyDefaultValues: Boolean): Builder = {
      applyDefaultValuesWhenDeserializing = applyDefaultValues
      this
    }

    override def shouldApplyDefaultValuesWhenDeserializing(): Boolean = applyDefaultValuesWhenDeserializing

    def addModule(module: JacksonModule): Builder = {
      module.initializers.result().foreach(init => initializers += init)
      this
    }

    def build(): JacksonModule = {
      val builderInstance = this
      val module = new ScalaAnnotationIntrospectorModuleInstance(builderInstance)
      initializers.result().foreach(init => module += init)
      module
    }
  }

  def builder(): Builder = new Builder()

  val defaultBuilder: ReadOnlyBuilder = builder()
}

