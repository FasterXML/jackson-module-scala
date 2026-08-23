package tools.jackson.module.scala

import tools.jackson.databind.JacksonModule.SetupContext
import tools.jackson.module.scala.deser.{ScalaNumberDeserializersModule, ScalaObjectDeserializerModule, UntypedObjectDeserializerModule}
import tools.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule

object ScalaModule {

  trait Config {
    /**
     * @return whether the module should support classes built with Scala 3 compiler (default: true)
     */
    def shouldSupportScala3Classes(): Boolean
    /**
     * @return whether the module should support deserializing null collections as empty (default: true)
     */
    def shouldDeserializeNullCollectionsAsEmpty(): Boolean
  }

  class Builder extends Config {
    private val modules = scala.collection.mutable.Buffer[JacksonModule]()

    /**
     * This builder's own annotation introspection, with caches, cache settings and referenced value
     * type registrations independent of every other builder and of the
     * [[tools.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule]] object.
     *
     * @since 3.3.0
     */
    val scalaAnnotationIntrospectorModule: ScalaAnnotationIntrospectorModule =
      ScalaAnnotationIntrospectorModule.newStandaloneInstance()

    /**
     * This builder's own automatic polymorphic support, with caches and cache settings independent
     * of every other builder and of the [[SealedPolymorphismModule]] object. Exposed so that it can
     * be configured for this build.
     *
     * @since 3.3.0
     */
    val sealedPolymorphismModule: SealedPolymorphismModule = new SealedPolymorphismModule {}
    private var supportScala3Classes = true
    private var deserializeNullCollectionsAsEmpty = true

    def supportScala3Classes(support: Boolean): Builder = {
      supportScala3Classes = support
      this
    }

    def deserializeNullCollectionsAsEmpty(asEmpty: Boolean): Builder = {
      deserializeNullCollectionsAsEmpty = asEmpty
      this
    }

    override def shouldSupportScala3Classes(): Boolean = supportScala3Classes

    override def shouldDeserializeNullCollectionsAsEmpty(): Boolean = deserializeNullCollectionsAsEmpty

    def addModule(module: JacksonModule): Builder = {
      // a module registered twice would contribute its serializers and deserializers twice over
      if (!hasModule(module)) modules.+=(module)
      this
    }

    def removeModule(module: JacksonModule): Builder = {
      val remaining = modules.filterNot(sameModule(_, module)).toList
      modules.clear()
      modules.++=(remaining)
      this
    }

    def hasModule(module: JacksonModule): Boolean = {
      modules.exists(sameModule(_, module))
    }

    // Some builtin modules are registered as an instance of their own, so that a built module keeps
    // state independent of the module object. Matching on the module name as well as on the
    // instance keeps `removeModule(EnumModule)` working for those.
    private def sameModule(existing: JacksonModule, module: JacksonModule): Boolean =
      existing == module || existing.getModuleName == module.getModuleName

    def addAllBuiltinModules(): Builder = {
      addModule(IteratorModule)
      addModule(EnumerationModule)
      addModule(OptionModule)
      addModule(SeqModule)
      addModule(TupleModule)
      addModule(MapModule)
      addModule(SetModule)
      addModule(ScalaNumberDeserializersModule)
      addModule(scalaAnnotationIntrospectorModule)
      addModule(ScalaObjectDeserializerModule)
      addModule(UntypedObjectDeserializerModule)
      addModule(EitherModule)
      addModule(SymbolModule)
      // this builder's own instance, so a built module keeps its own polymorphism state
      addModule(sealedPolymorphismModule)
      BuiltinModules.addScalaVersionSpecificModules(this)
      this
    }

    def build(): JacksonModule = {
      val configInstance = this
      new JacksonModule {
        override def getModuleName: String = "ScalaModule"

        override val config: Builder = configInstance
        override def getInitializers(config: Config): Seq[SetupContext => Unit] = {
          modules.toSeq.flatMap(_.getInitializers(config))
        }
      }
    }
  }

  def builder(): Builder = new Builder()

  val defaultBuilder: Config = new Config {
    override def shouldSupportScala3Classes(): Boolean = true
    override def shouldDeserializeNullCollectionsAsEmpty(): Boolean = true
  }
}

