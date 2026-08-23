package tools.jackson.module.scala.ser

import tools.jackson.databind.ser.ValueSerializerModifier
import tools.jackson.databind.{BeanDescription, SerializationConfig, ValueSerializer}
import tools.jackson.databind.JacksonModule.SetupContext
import tools.jackson.module.scala.{JacksonModule, ScalaModule}
import tools.jackson.module.scala.JacksonModule.InitializerBuilder
import tools.jackson.module.scala.util.SimplePolymorphism

private class SimplePolymorphismSerializerModifier(config: ScalaModule.Config) extends ValueSerializerModifier {
  override def modifySerializer(config: SerializationConfig, beanDesc: BeanDescription.Supplier,
                                serializer: ValueSerializer[_]): ValueSerializer[_] = {
    val rawClass = beanDesc.getBeanClass
    if (SimplePolymorphism.conflictingJsonTypeInfo(rawClass)) {
      throw new IllegalArgumentException(SimplePolymorphism.conflictMessage(rawClass))
    }
    // base types are never written directly - only the implementation dispatched to at runtime
    if (SimplePolymorphism.isSupported(rawClass) && !SimplePolymorphism.isBaseType(rawClass)) {
      new TypeTaggedSerializer(SimplePolymorphism.TypePropertyName, SimplePolymorphism.typeNameFor(rawClass),
        serializer.asInstanceOf[ValueSerializer[AnyRef]])
    } else serializer
  }
}

trait SimplePolymorphismSerializerModule extends JacksonModule {
  override def getModuleName: String = "SimplePolymorphismSerializerModule"

  override def getInitializers(config: ScalaModule.Config): Seq[SetupContext => Unit] = {
    val builder = new InitializerBuilder()
    builder += new SimplePolymorphismSerializerModifier(config)
    builder.build()
  }
}

object SimplePolymorphismSerializerModule extends SimplePolymorphismSerializerModule
