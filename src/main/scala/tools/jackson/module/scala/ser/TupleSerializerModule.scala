package tools.jackson.module.scala.ser

import com.fasterxml.jackson.annotation.JsonFormat
import tools.jackson.core.{JsonGenerator, JsonToken}
import tools.jackson.databind.JacksonModule.SetupContext
import tools.jackson.databind._
import tools.jackson.databind.jsontype.TypeSerializer
import tools.jackson.databind.ser.Serializers
import tools.jackson.module.scala.JacksonModule.InitializerBuilder
import tools.jackson.module.scala.{JacksonModule, ScalaModule}

import scala.languageFeature.postfixOps

private class TupleSerializer(tupleType: JavaType, elementTypeSerializers: IndexedSeq[TypeSerializer])
  extends ValueSerializer[Product] {

  def this(tupleType: JavaType) = this(tupleType, IndexedSeq.empty)

  // an element is typed as its declared type would be, as TupleDeserializer expects
  override def createContextual(ctxt: SerializationContext, property: BeanProperty): ValueSerializer[_] = {
    val typeSers = (0 until tupleType.containedTypeCount).map { i =>
      val elementType = tupleType.containedType(i)
      if (property == null) ctxt.findTypeSerializer(elementType)
      else ctxt.findPropertyTypeSerializer(elementType, property.getMember)
    }
    if (typeSers.forall(_ == null)) this else new TupleSerializer(tupleType, typeSers)
  }

  def serialize(value: Product, jgen: JsonGenerator, serializationContext: SerializationContext): Unit = {
    jgen.writeStartArray(value)
    serializeContents(value, jgen, serializationContext)
    jgen.writeEndArray()
  }

  override def serializeWithType(value: Product, jgen: JsonGenerator, serializationContext: SerializationContext,
                                 typeSer: TypeSerializer): Unit = {
    val typeIdDef = typeSer.writeTypePrefix(jgen, serializationContext, typeSer.typeId(value, JsonToken.START_ARRAY))
    jgen.assignCurrentValue(value)
    serializeContents(value, jgen, serializationContext)
    typeSer.writeTypeSuffix(jgen, serializationContext, typeIdDef)
  }

  private def serializeContents(value: Product, jgen: JsonGenerator, serializationContext: SerializationContext): Unit = {
    var i = 0
    value.productIterator.foreach { element =>
      val elementTypeSer = if (i < elementTypeSerializers.length) elementTypeSerializers(i) else None.orNull
      if (element == null || elementTypeSer == null) {
        // typed by its runtime class alone, as a root value is: a class's own @JsonTypeInfo still applies
        serializationContext.writeValue(jgen, element)
      } else {
        val ref = element.asInstanceOf[AnyRef]
        serializationContext.findValueSerializer(ref.getClass).serializeWithType(ref, jgen, serializationContext, elementTypeSer)
      }
      i += 1
    }
  }
}

private class TupleSerializerResolver(config: ScalaModule.Config) extends Serializers.Base {

  private val PRODUCT = classOf[Product]

  override def findSerializer(serializationConfig: SerializationConfig, javaType: JavaType, beanDesc: BeanDescription.Supplier,
                              formatOverrides: JsonFormat.Value) = {
    val cls = javaType.getRawClass
    if (!PRODUCT.isAssignableFrom(cls)) null else
    // If it's not *actually* a tuple, it's either a case class or a custom Product
    // which either way we shouldn't handle here.
    if (!cls.getName.startsWith("scala.Tuple")) null else
    new TupleSerializer(javaType)
  }

}

trait TupleSerializerModule extends JacksonModule {
  override def getModuleName: String = "TupleSerializerModule"

  override def getInitializers(config: ScalaModule.Config): Seq[SetupContext => Unit] = {
    val builder = new InitializerBuilder()
    builder += new TupleSerializerResolver(config)
    builder.build()
  }
}

object TupleSerializerModule extends TupleSerializerModule
