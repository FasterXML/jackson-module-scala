package com.fasterxml.jackson.module.scala

import java.io.{File, InputStream, Reader}
import java.net.URL

import com.fasterxml.jackson.core.{JsonParser, TreeNode}
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper
import com.fasterxml.jackson.databind.jsonschema.JsonSchema
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.json.JsonMapper

object ScalaObjectMapper {
  def ::(o: JsonMapper) = new Mixin(o)
  final class Mixin private[ScalaObjectMapper](mapper: JsonMapper)
    extends JsonMapper(mapper.rebuild().build()) with ScalaObjectMapper
}

@deprecated("ScalaObjectMapper is deprecated because Manifests are not supported in Scala3", "2.12.1")
trait ScalaObjectMapper {
  self: ObjectMapper =>

  /*
   **********************************************************
   * Configuration: mix-in annotations
   **********************************************************
   */

  /**
   * Method to use for adding mix-in annotations to use for augmenting
   * specified class or interface. All annotations from
   * <code>mixinSource</code> are taken to override annotations
   * that <code>target</code> (or its supertypes) has.
   *
   * @tparam Target Class (or interface) whose annotations to effectively override
   * @tparam MixinSource Class (or interface) whose annotations are to
   *                     be "added" to target's annotations, overriding as necessary
   */
  @deprecated("this support in jackson-databind is moving to the MapperBuilder", "2.12.2")
  final def addMixin[Target: Manifest, MixinSource: Manifest]() = {
    addMixIn(manifest[Target].runtimeClass, manifest[MixinSource].runtimeClass)
  }

  /**
   * @deprecated Since 2.5: replaced by a fluent form of the method; { @link #addMixIn(Class, Class)}.
   */
  @deprecated("use addMixIn", "2.5")
  final def addMixInAnnotations[Target: Manifest, MixinSource: Manifest]() = {
    addMixIn(manifest[Target].runtimeClass, manifest[MixinSource].runtimeClass)
  }

  @deprecated("this support in jackson-databind is moving to the MapperBuilder", "2.12.2")
  final def findMixInClassFor[T: Manifest]: Class[_] = {
    findMixInClassFor(manifest[T].runtimeClass)
  }

  /*
   **********************************************************
   * Configuration, basic type handling
   **********************************************************
   */

  /**
   * Convenience method for constructing [[com.fasterxml.jackson.databind.JavaType]] out of given
   * type (typically <code>java.lang.Class</code>), but without explicit
   * context.
   */
  def constructType[T](implicit m: Manifest[T]): JavaType = {
    val clazz = m.runtimeClass
    if (isArray(clazz)) {
      val typeArguments = m.typeArguments.map(constructType(_)).toArray
      if (typeArguments.length != 1) {
        throw new IllegalArgumentException("Need exactly 1 type parameter for array like types ("+clazz.getName+")")
      }
      getTypeFactory.constructArrayType(typeArguments(0))
    } else if (isMapLike(clazz)) {
      val typeArguments = m.typeArguments.map(constructType(_)).toArray
      if (typeArguments.length != 2) {
        throw new IllegalArgumentException("Need exactly 2 type parameters for map like types ("+clazz.getName+")")
      }
      getTypeFactory.constructMapLikeType(clazz, typeArguments(0), typeArguments(1))
    } else if (isReference(clazz)) { // Option is a subclass of IterableOnce, so check it first
      val typeArguments = m.typeArguments.map(constructType(_)).toArray
      if (typeArguments.length != 1) {
        throw new IllegalArgumentException("Need exactly 1 type parameter for reference types ("+clazz.getName+")")
      }
      getTypeFactory.constructReferenceType(clazz, typeArguments(0))
    } else if (isCollectionLike(clazz)) {
      val typeArguments = m.typeArguments.map(constructType(_)).toArray
      if (typeArguments.length != 1) {
        throw new IllegalArgumentException("Need exactly 1 type parameter for collection like types ("+clazz.getName+")")
      }
      getTypeFactory.constructCollectionLikeType(clazz, typeArguments(0))
    } else {
      val typeArguments = m.typeArguments.map(constructType(_)).toArray
      getTypeFactory.constructParametricType(clazz, typeArguments: _*)
    }
  }

  /*
   **********************************************************
   * Public API (from ObjectCodec): deserialization
   * (mapping from JSON to Java types);
   * main methods
   **********************************************************
   */

  /**
   * Method to deserialize JSON content into a Java type, reference
   * to which is passed as argument. Type is passed using so-called
   * "super type token" (see )
   * and specifically needs to be used if the root type is a
   * parameterized (generic) container type.
   */
  def readValue[T: Manifest](jp: JsonParser): T = {
    readValue(jp, constructType[T])
  }

  /**
   * Method for reading sequence of Objects from parser stream.
   * Sequence can be either root-level "unwrapped" sequence (without surrounding
   * JSON array), or a sequence contained in a JSON Array.
   * In either case [[com.fasterxml.jackson.core.JsonParser]] must point to the first token of
   * the first element, OR not point to any token (in which case it is advanced
   * to the next token). This means, specifically, that for wrapped sequences,
   * parser MUST NOT point to the surrounding <code>START_ARRAY</code> but rather
   * to the token following it.
   * <p>
   * Note that [[com.fasterxml.jackson.databind.ObjectReader]] has more complete set of variants.
   */
  def readValues[T: Manifest](jp: JsonParser): MappingIterator[T] = {
    readValues(jp, constructType[T])
  }

  /*
   **********************************************************
   * Public API (from ObjectCodec): Tree Model support
   **********************************************************
   */

  /**
   * Convenience conversion method that will bind data given JSON tree
   * contains into specific value (usually bean) type.
   * <p>
   * Equivalent to:
   * <pre>
   * objectMapper.convertValue(n, valueClass);
   * </pre>
   */
  def treeToValue[T: Manifest](n: TreeNode): T = {
    treeToValue(n, manifest[T].runtimeClass).asInstanceOf[T]
  }

  /*
   **********************************************************
   * Extended Public API, accessors
   **********************************************************
   */

  /**
   * Method that can be called to check whether mapper thinks
   * it could serialize an instance of given Class.
   * Check is done
   * by checking whether a serializer can be found for the type.
   *
   * @return True if mapper can find a serializer for instances of
   *         given class (potentially serializable), false otherwise (not
   *         serializable)
   * @deprecated jackson-databind will not implement this in v3.0.0
   */
  @deprecated("jackson-databind will not implement this in v3.0.0", "2.12.1")
  def canSerialize[T: Manifest]: Boolean = {
    canSerialize(manifest[T].runtimeClass)
  }

  /**
   * Method that can be called to check whether mapper thinks
   * it could deserialize an Object of given type.
   * Check is done
   * by checking whether a deserializer can be found for the type.
   *
   * @return True if mapper can find a serializer for instances of
   *         given class (potentially serializable), false otherwise (not
   *         serializable)
   * @deprecated jackson-databind will not implement this in v3.0.0
   */
  @deprecated("jackson-databind will not implement this in v3.0.0", "2.12.1")
  def canDeserialize[T: Manifest]: Boolean = {
    canDeserialize(constructType[T])
  }

  /*
   **********************************************************
   * Extended Public API, deserialization,
   * convenience methods
   **********************************************************
   */
  def readValue[T: Manifest](src: File): T = {
    readValue(src, constructType[T])
  }

  def readValue[T: Manifest](src: URL): T = {
    readValue(src, constructType[T])
  }

  def readValue[T: Manifest](content: String): T = {
    readValue(content, constructType[T])
  }

  def readValue[T: Manifest](src: Reader): T = {
    readValue(src, constructType[T])
  }

  def readValue[T: Manifest](src: InputStream): T = {
    readValue(src, constructType[T])
  }

  def readValue[T: Manifest](src: Array[Byte]): T = {
    readValue(src, constructType[T])
  }

  def readValue[T: Manifest](src: Array[Byte], offset: Int, len: Int): T = {
    readValue(src, offset, len, constructType[T])
  }

  def updateValue[T: Manifest](valueToUpdate: T, src: File): T = {
    objectReaderFor(valueToUpdate).readValue(src)
  }

  def updateValue[T: Manifest](valueToUpdate: T, src: URL): T = {
    objectReaderFor(valueToUpdate).readValue(src)
  }

  def updateValue[T: Manifest](valueToUpdate: T, content: String): T = {
    objectReaderFor(valueToUpdate).readValue(content)
  }

  def updateValue[T: Manifest](valueToUpdate: T, src: Reader): T = {
    objectReaderFor(valueToUpdate).readValue(src)
  }

  def updateValue[T: Manifest](valueToUpdate: T, src: InputStream): T = {
    objectReaderFor(valueToUpdate).readValue(src)
  }

  def updateValue[T: Manifest](valueToUpdate: T, src: Array[Byte]): T = {
    objectReaderFor(valueToUpdate).readValue(src)
  }

  def updateValue[T: Manifest](valueToUpdate: T, src: Array[Byte], offset: Int, len: Int): T = {
    objectReaderFor(valueToUpdate).readValue(src, offset, len)
  }

  private def objectReaderFor[T: Manifest](valueToUpdate: T): ObjectReader = {
    readerForUpdating(valueToUpdate).forType(constructType[T])
  }

  /*
   **********************************************************
   * Extended Public API: constructing ObjectWriters
   * for more advanced configuration
   **********************************************************
   */

  /**
   * Factory method for constructing [[com.fasterxml.jackson.databind.ObjectWriter]] that will
   * serialize objects using specified JSON View (filter).
   */
  def writerWithView[T: Manifest]: ObjectWriter = {
    writerWithView(manifest[T].runtimeClass)
  }

  /**
   * @deprecated Since 2.5, use { @link #writerFor(Class)} instead
   */
  @deprecated(message = "Replaced with writerFor", since = "2.5")
  def writerWithType[T: Manifest]: ObjectWriter = {
    writerFor[T]
  }

  /**
   * Factory method for constructing {@link com.fasterxml.jackson.databind.ObjectWriter} that will
   * serialize objects using specified root type, instead of actual
   * runtime type of value. Type must be a super-type of runtime type.
   * <p>
   * Main reason for using this method is performance, as writer is able
   * to pre-fetch serializer to use before write, and if writer is used
   * more than once this avoids addition per-value serializer lookups.
   *
   * @since 2.5
   */
  def writerFor[T: Manifest]: ObjectWriter = {
    writerFor(constructType[T])
  }

  /*
   **********************************************************
   * Extended Public API: constructing ObjectReaders
   * for more advanced configuration
   **********************************************************
   */

  /**
   * Factory method for constructing [[com.fasterxml.jackson.databind.ObjectReader]] that will
   * read or update instances of specified type
   */
  @deprecated(message = "Replaced with readerFor", since = "2.6")
  def reader[T: Manifest]: ObjectReader = {
    reader(constructType[T])
  }

  /**
   * Factory method for constructing [[com.fasterxml.jackson.databind.ObjectReader]] that will
   * read or update instances of specified type
   */
  def readerFor[T: Manifest]: ObjectReader = {
    readerFor(constructType[T])
  }

  /**
   * Factory method for constructing [[com.fasterxml.jackson.databind.ObjectReader]] that will
   * deserialize objects using specified JSON View (filter).
   */
  def readerWithView[T: Manifest]: ObjectReader = {
    readerWithView(manifest[T].runtimeClass)
  }

  /*
   **********************************************************
   * Extended Public API: convenience type conversion
   **********************************************************
   */

  /**
   * Convenience method for doing two-step conversion from given value, into
   * instance of given value type. This is functionality equivalent to first
   * serializing given value into JSON, then binding JSON data into value
   * of given type, but may be executed without fully serializing into
   * JSON. Same converters (serializers, deserializers) will be used as for
   * data binding, meaning same object mapper configuration works.
   *
   * @throws IllegalArgumentException If conversion fails due to incompatible type;
   *                                  if so, root cause will contain underlying checked exception data binding
   *                                  functionality threw
   */
  def convertValue[T: Manifest](fromValue: Any): T = {
    convertValue(fromValue, constructType[T])
  }

  /*
   **********************************************************
   * Extended Public API: JSON Schema generation
   **********************************************************
   */

  /**
   * Generate <a href="http://json-schema.org/">Json-schema</a>
   * instance for specified class.
   *
   * @tparam T The class to generate schema for
   * @return Constructed JSON schema.
   */
  @deprecated("JsonSchema is deprecated in favor of JsonFormatVisitor", "2.2.2")
  def generateJsonSchema[T: Manifest]: JsonSchema = {
    generateJsonSchema(manifest[T].runtimeClass)
  }

  /**
   * Method for visiting type hierarchy for given type, using specified visitor.
   * <p>
   * This method can be used for things like
   * generating <a href="http://json-schema.org/">Json Schema</a>
   * instance for specified type.
   *
   * @tparam T Type to generate schema for (possibly with generic signature)
   *
   * @since 2.1
   */
  def acceptJsonFormatVisitor[T: Manifest](visitor: JsonFormatVisitorWrapper): Unit = {
    acceptJsonFormatVisitor(manifest[T].runtimeClass, visitor)
  }

  private def isArray(c: Class[_]): Boolean = {
    c.isArray
  }

  private val MAP = classOf[collection.Map[_,_]]
  private def isMapLike(c: Class[_]): Boolean = {
    MAP.isAssignableFrom(c)
  }

  private val OPTION = classOf[Option[_]]
  private def isReference(c: Class[_]): Boolean = {
     OPTION.isAssignableFrom(c)
  }

  private val ITERABLE = classOf[collection.Iterable[_]]
  private def isCollectionLike(c: Class[_]): Boolean = {
    ITERABLE.isAssignableFrom(c)
  }
}
