package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.databind.Module.SetupContext
import com.fasterxml.jackson.module.scala.deser.{ScalaNumberDeserializersModule, ScalaObjectDeserializerModule, UntypedObjectDeserializerModule}
import com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule

object ScalaModule {

  class Builder {
    private val modules = scala.collection.mutable.Buffer[JacksonModule]()

    def addModule(module: JacksonModule): Builder = {
      modules.+=(module)
      this
    }

    def removeModule(module: JacksonModule): Builder = {
      modules.-=(module)
      this
    }

    def hasModule(module: JacksonModule): Boolean = {
      modules.contains(module)
    }

    def addAllBuiltinModules(): Builder = {
      addModule(IteratorModule)
      addModule(EnumerationModule)
      addModule(OptionModule)
      addModule(SeqModule)
      addModule(IterableModule)
      addModule(TupleModule)
      addModule(MapModule)
      addModule(SetModule)
      addModule(ScalaNumberDeserializersModule)
      addModule(ScalaAnnotationIntrospectorModule)
      addModule(ScalaObjectDeserializerModule)
      addModule(UntypedObjectDeserializerModule)
      addModule(EitherModule)
      addModule(SymbolModule)
      BuiltinModules.addScalaVersionSpecificModules(this)
      this
    }

    def build(): JacksonModule = new JacksonModule {
      override def setupModule(context: SetupContext): Unit = {
        modules.foreach(_.setupModule(context))
      }
    }
  }

  def builder(): Builder = new Builder()
}

