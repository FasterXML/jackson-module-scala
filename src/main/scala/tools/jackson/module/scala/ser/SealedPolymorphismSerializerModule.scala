package tools.jackson.module.scala.ser

import tools.jackson.databind.ser.ValueSerializerModifier
import tools.jackson.databind.{BeanDescription, SerializationConfig, ValueSerializer}
import tools.jackson.databind.JacksonModule.SetupContext
import tools.jackson.module.scala.{JacksonModule, ScalaModule, SealedPolymorphismSupportState}
import tools.jackson.module.scala.JacksonModule.InitializerBuilder
import tools.jackson.module.scala.util.SealedPolymorphism

private class SealedPolymorphismSerializerModifier(config: ScalaModule.Config, polymorphism: SealedPolymorphism)
  extends ValueSerializerModifier {
  override def modifySerializer(config: SerializationConfig, beanDesc: BeanDescription.Supplier,
                                serializer: ValueSerializer[_]): ValueSerializer[_] = {
    val rawClass = beanDesc.getBeanClass
    if (polymorphism.conflictingJsonTypeInfo(rawClass)) {
      throw new IllegalArgumentException(SealedPolymorphism.conflictMessage(rawClass))
    }
    // base types are never written directly - only the implementation dispatched to at runtime
    if (polymorphism.isSupported(rawClass) && !polymorphism.isBaseType(rawClass)) {
      // refuse to write a value that could not be read back
      polymorphism.unreachableReason(rawClass).foreach(reason => throw new IllegalArgumentException(reason))
      new TypeTaggedSerializer(SealedPolymorphism.TypePropertyName, SealedPolymorphism.typeNameFor(rawClass),
        serializer.asInstanceOf[ValueSerializer[AnyRef]])
    } else serializer
  }
}

trait SealedPolymorphismSerializerModule extends JacksonModule with SealedPolymorphismSupportState {
  override def getModuleName: String = "SealedPolymorphismSerializerModule"

  protected def serializerInitializers(config: ScalaModule.Config): Seq[SetupContext => Unit] = {
    val builder = new InitializerBuilder()
    builder += new SealedPolymorphismSerializerModifier(config, sealedPolymorphism)
    builder.build()
  }

  override def getInitializers(config: ScalaModule.Config): Seq[SetupContext => Unit] =
    serializerInitializers(config)
}

object SealedPolymorphismSerializerModule extends SealedPolymorphismSerializerModule
