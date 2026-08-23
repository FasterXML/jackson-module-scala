package tools.jackson.module.scala

private[scala] object BuiltinModules {
  def addScalaVersionSpecificModules(builder: ScalaModule.Builder): ScalaModule.Builder = {
    // a fresh instance, so a built module keeps its own enum state
    builder.addModule(new EnumModule {})
    builder.addModule(SimplePolymorphismModule)
    builder
  }
}
