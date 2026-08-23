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
    if (polymorphism.conflictingJsonTypeInfo(rawClass, config)) {
      throw new IllegalArgumentException(polymorphism.conflictMessage(rawClass, config))
    }
    // base types are never written directly - only the implementation dispatched to at runtime
    if (polymorphism.isSupported(rawClass, config) && !polymorphism.isBaseType(rawClass, config)) {
      // refuse to write a value that could not be read back
      polymorphism.unreachableReason(rawClass, config).foreach(reason => throw new IllegalArgumentException(reason))
      val typeName = polymorphism.typeNameFor(rawClass, polymorphism.rootOf(rawClass, config))
      new TypeTaggedSerializer(SealedPolymorphism.TypePropertyName, typeName,
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
