package com.fasterxml.jackson.module.scala.deser

import org.codehaus.jackson.`type`.JavaType
import org.codehaus.jackson.map._
import deser.CollectionDeserializer
import java.lang.reflect.Constructor
import collection.JavaConversions._
import collection.mutable.ListBuffer
import org.codehaus.jackson._
import java.util.{ArrayList, Collection}

/**
 * The ScalaListDeserializer deserializes json arrays into Scala Iterables.
 */
class ScalaListDeserializer(val collectionType: JavaType,
							val valueDeser: JsonDeserializer[Object],
							val valueTypeDeser: TypeDeserializer) extends JsonDeserializer[collection.Iterable[Object]] {

	// Note/Todo: I'm making an assumption here: I know the implementation of the CollectionDeserializer doesn't use the
	// Constructor if I don't call the main deserialize() method, but instead call the one where I can pass in a Collection.
	val clazz = classOf[ArrayList[Object]];
	val constructors = clazz.getConstructors.iterator
	val constructor: Constructor[Collection[Object]] = constructors.next.asInstanceOf[Constructor[Collection[Object]]]

	// The wrapped deserializer. I'll just use it, rather than copy-and-paste. Keep it DRY.
	val javaCollectionDeserializer = new CollectionDeserializer(collectionType, valueDeser, valueTypeDeser, constructor)

	override def deserialize(jp: JsonParser, ctxt: DeserializationContext) : collection.Iterable[Object] = {

		// Todo: Here we're just creating our own Iterable implementation. The JavaType is available, if we wanted to do
		// more fine-tuned introspection to determine what specific type of Iterable to instantiate. There are several
		// Java-Scala conversions available; we just want a thin facade implementation of the Java interface that simply
		// calls the associated Scala collection methods. Shouldn't really be any speed degradation here.
		val list = new ListBuffer[Object]()
		val collection = asJavaList(list)
		javaCollectionDeserializer.deserialize(jp, ctxt, collection)

		// mutable.List and immutable.List are not compatible; if the requested type is immutable, just return the
		// mutable ListBuffer as a immutable Iterable List.
		if (isImmutableTypeRequested) {
			list.toList
		} else {
			list
		}
	}

	private def isImmutableTypeRequested = {
		classOf[collection.immutable.List[Object]].isAssignableFrom(collectionType.getRawClass)
	}
}