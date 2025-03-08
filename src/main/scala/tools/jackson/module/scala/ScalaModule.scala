package tools.jackson.module.scala

import tools.jackson.databind.JacksonModule.SetupContext
import tools.jackson.module.scala.deser.{ScalaNumberDeserializersModule, ScalaObjectDeserializerModule, UntypedObjectDeserializerModule}
import tools.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule

object ScalaModule {

  trait Config {
    def shouldApplyDefaultValuesWhenDeserializing(): Boolean
    def shouldSupportScala3Classes(): Boolean
  }

  class Builder extends Config {
    private val modules = scala.collection.mutable.Buffer[JacksonModule]()
    private var applyDefaultValuesWhenDeserializing = true
    private var supportScala3Classes = true

    def applyDefaultValuesWhenDeserializing(applyDefaultValues: Boolean): Builder = {
      applyDefaultValuesWhenDeserializing = applyDefaultValues
      this
    }

    def supportScala3Classes(support: Boolean): Builder = {
      supportScala3Classes = support
      this
    }

    override def shouldApplyDefaultValuesWhenDeserializing(): Boolean = applyDefaultValuesWhenDeserializing
    override def shouldSupportScala3Classes(): Boolean = supportScala3Classes

    def addModule(module: JacksonModule): Builder = {
      modules.addOne(module)
      this
    }

    def removeModule(module: JacksonModule): Builder = {
      modules.subtractOne(module)
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

    def build(): JacksonModule = {
      val configInstance = this
      val module = new JacksonModule {
        override val config = configInstance
        override def getInitializers(config: Config): Seq[SetupContext => Unit] = {
          modules.toSeq.flatMap(_.getInitializers(config))
        }
      }
      module
    }
  }

  def builder(): Builder = new Builder()

  val defaultBuilder: Config = new Config {
    override def shouldApplyDefaultValuesWhenDeserializing(): Boolean = true
    override def shouldSupportScala3Classes(): Boolean = true
  }
}

