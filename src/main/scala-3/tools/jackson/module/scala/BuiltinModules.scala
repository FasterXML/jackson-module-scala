package tools.jackson.module.scala

private[scala] object BuiltinModules {
  def addScalaVersionSpecificModules(builder: ScalaModule.Builder): ScalaModule.Builder = {
    builder.addModule(EnumModule)
    builder
  }
}
