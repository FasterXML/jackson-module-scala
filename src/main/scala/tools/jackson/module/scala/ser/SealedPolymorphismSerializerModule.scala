package tools.jackson.module.scala.ser

import tools.jackson.databind.ser.ValueSerializerModifier
import tools.jackson.databind.{BeanDescription, SerializationConfig, ValueSerializer}
import tools.jackson.databind.JacksonModule.SetupContext
import tools.jackson.module.scala.{JacksonModule, ScalaModule}
import tools.jackson.module.scala.JacksonModule.InitializerBuilder
import tools.jackson.module.scala.util.SealedPolymorphism

private class SealedPolymorphismSerializerModifier(config: ScalaModule.Config) extends ValueSerializerModifier {
  override def modifySerializer(config: SerializationConfig, beanDesc: BeanDescription.Supplier,
                                serializer: ValueSerializer[_]): ValueSerializer[_] = {
    val rawClass = beanDesc.getBeanClass
    if (SealedPolymorphism.conflictingJsonTypeInfo(rawClass)) {
      throw new IllegalArgumentException(SealedPolymorphism.conflictMessage(rawClass))
    }
    // base types are never written directly - only the implementation dispatched to at runtime
    if (SealedPolymorphism.isSupported(rawClass) && !SealedPolymorphism.isBaseType(rawClass)) {
      // refuse to write a value that could not be read back
      SealedPolymorphism.unreachableReason(rawClass).foreach(reason => throw new IllegalArgumentException(reason))
      new TypeTaggedSerializer(SealedPolymorphism.TypePropertyName, SealedPolymorphism.typeNameFor(rawClass),
        serializer.asInstanceOf[ValueSerializer[AnyRef]])
    } else serializer
  }
}

trait SealedPolymorphismSerializerModule extends JacksonModule {
  override def getModuleName: String = "SealedPolymorphismSerializerModule"

  override def getInitializers(config: ScalaModule.Config): Seq[SetupContext => Unit] = {
    val builder = new InitializerBuilder()
    builder += new SealedPolymorphismSerializerModifier(config)
    builder.build()
  }
}

object SealedPolymorphismSerializerModule extends SealedPolymorphismSerializerModule
