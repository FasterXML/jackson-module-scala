package tools.jackson.module.scala.ser

import tools.jackson.core.{JsonGenerator, JsonToken}
import tools.jackson.databind.jsontype.TypeSerializer
import tools.jackson.databind.ser.std.ToEmptyObjectSerializer
import tools.jackson.databind.util.NameTransformer
import tools.jackson.databind.{BeanProperty, SerializationContext, ValueSerializer}

/**
 * Wraps the bean serializer of a concrete type so that a type name is written as the first property
 * of the object, making the value round-trippable without `@JsonTypeInfo` annotations.
 *
 * The delegate is asked for an unwrapping view of itself, which writes the properties without the
 * enclosing braces, so the tag can be emitted ahead of them.
 *
 * A type Jackson found nothing to write for - a `case object`, typically - is not given a bean
 * serializer but a [[ToEmptyObjectSerializer]], which writes `{}` and cannot unwrap. The braces are
 * written here, so for such a delegate the tag is all there is. Do not rely on a case object getting
 * a bean serializer: it does when a supertype carries an annotation Jackson collects, which
 * `scala.Product` does when it comes from the Scala 2 standard library but not from Scala 3's.
 */
private[scala] class TypeTaggedSerializer(typeProperty: String, typeName: String, rootClass: Class[_],
                                          delegate: ValueSerializer[AnyRef]) extends ValueSerializer[AnyRef] {

  // at root level the value is typed as the root of its hierarchy would be, as a collection is
  private val rootTypeSerializers = new RootTypeSerializers(_ => rootClass)

  // resolved lazily - the delegate has to be resolved before it can hand out an unwrapping view
  private lazy val unwrapped: Option[ValueSerializer[AnyRef]] = delegate match {
    case _: ToEmptyObjectSerializer => None
    case _ => Some(delegate.unwrappingSerializer(NameTransformer.NOP))
  }

  override def resolve(serializationContext: SerializationContext): Unit = delegate.resolve(serializationContext)

  override def createContextual(serializationContext: SerializationContext, property: BeanProperty): ValueSerializer[_] = {
    val contextual = delegate.createContextual(serializationContext, property)
    if (contextual eq delegate) this
    else new TypeTaggedSerializer(typeProperty, typeName, rootClass, contextual.asInstanceOf[ValueSerializer[AnyRef]])
  }

  override def serialize(value: AnyRef, jgen: JsonGenerator, serializationContext: SerializationContext): Unit = {
    val typeSer = rootTypeSerializers.rootTypeSerializer(jgen, serializationContext, value)
    if (typeSer != null) {
      serializeWithType(value, jgen, serializationContext, typeSer)
    } else {
      jgen.writeStartObject(value)
      jgen.writeStringProperty(typeProperty, typeName)
      unwrapped.foreach(_.serialize(value, jgen, serializationContext))
      jgen.writeEndObject()
    }
  }

  // a type id names the implementation exactly, so the tag is not written as well: the prefix opens
  // the object (or the wrapper around it) and the suffix closes what the prefix opened
  override def serializeWithType(value: AnyRef, jgen: JsonGenerator, serializationContext: SerializationContext,
                                 typeSer: TypeSerializer): Unit = {
    val typeIdDef = typeSer.writeTypePrefix(jgen, serializationContext, typeSer.typeId(value, JsonToken.START_OBJECT))
    jgen.assignCurrentValue(value)
    unwrapped.foreach(_.serialize(value, jgen, serializationContext))
    typeSer.writeTypeSuffix(jgen, serializationContext, typeIdDef)
  }
}
