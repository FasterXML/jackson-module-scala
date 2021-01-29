package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.core.{JsonParser, TreeNode}
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.json.JsonMapper

import java.io.{File, InputStream, Reader}
import java.net.URL
import scala.reflect.ClassTag

object ClassTagExtensions {
  def ::(o: JsonMapper) = new Mixin(o)
  final class Mixin private[ClassTagExtensions](mapper: JsonMapper)
    extends JsonMapper(mapper.rebuild().build()) with ClassTagExtensions
}

/**
 * Experimental ClassTag equivalent of ScalaObjectMapper. This does not do a good job with
 * reference types that wrap primitives, eg Option[Int], Seq[Boolean].
 *
 * This is because ClassTags only provide access to the Java class and information
 * about the wrapped types is lost due to type erasure.
 */
trait ClassTagExtensions {
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
  final def addMixin[Target: ClassTag, MixinSource: ClassTag](): ObjectMapper = {
    addMixIn(classFor[Target], classFor[MixinSource])
  }

  @deprecated("this support in jackson-databind is moving to the MapperBuilder", "2.12.2")
  final def findMixInClassFor[T: ClassTag]: Class[_] = {
    findMixInClassFor(classFor[T])
  }

  /*
   **********************************************************
   * Configuration, basic type handling
   **********************************************************
   */

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
  def readValue[T: ClassTag](jp: JsonParser): T = {
    readValue(jp, classFor[T])
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
  def readValues[T: ClassTag](jp: JsonParser): MappingIterator[T] = {
    readValues(jp, classFor[T])
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
  def treeToValue[T: ClassTag](n: TreeNode): T = {
    treeToValue(n, classFor[T])
  }

  /*
   **********************************************************
   * Extended Public API, deserialization,
   * convenience methods
   **********************************************************
   */
  def readValue[T: ClassTag](src: File): T = {
    readValue(src, classFor[T])
  }

  def readValue[T: ClassTag](src: URL): T = {
    readValue(src, classFor[T])
  }

  def readValue[T: ClassTag](content: String): T = {
    readValue(content, classFor[T])
  }

  def readValue[T: ClassTag](src: Reader): T = {
    readValue(src, classFor[T])
  }

  def readValue[T: ClassTag](src: InputStream): T = {
    readValue(src, classFor[T])
  }

  def readValue[T: ClassTag](src: Array[Byte]): T = {
    readValue(src, classFor[T])
  }

  def readValue[T: ClassTag](src: Array[Byte], offset: Int, len: Int): T = {
    readValue(src, offset, len, classFor[T])
  }

  def updateValue[T: ClassTag](valueToUpdate: T, src: File): T = {
    objectReaderFor(valueToUpdate).readValue(src)
  }

  def updateValue[T: ClassTag](valueToUpdate: T, src: URL): T = {
    objectReaderFor(valueToUpdate).readValue(src)
  }

  def updateValue[T: ClassTag](valueToUpdate: T, content: String): T = {
    objectReaderFor(valueToUpdate).readValue(content)
  }

  def updateValue[T: ClassTag](valueToUpdate: T, src: Reader): T = {
    objectReaderFor(valueToUpdate).readValue(src)
  }

  def updateValue[T: ClassTag](valueToUpdate: T, src: InputStream): T = {
    objectReaderFor(valueToUpdate).readValue(src)
  }

  def updateValue[T: ClassTag](valueToUpdate: T, src: Array[Byte]): T = {
    objectReaderFor(valueToUpdate).readValue(src)
  }

  def updateValue[T: ClassTag](valueToUpdate: T, src: Array[Byte], offset: Int, len: Int): T = {
    objectReaderFor(valueToUpdate).readValue(src, offset, len)
  }

  private def objectReaderFor[T: ClassTag](valueToUpdate: T): ObjectReader = {
    readerForUpdating(valueToUpdate).forType(classFor[T])
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
  def writerWithView[T: ClassTag]: ObjectWriter = {
    writerWithView(classFor[T])
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
  def writerFor[T: ClassTag]: ObjectWriter = {
    writerFor(classFor[T])
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
  def readerFor[T: ClassTag]: ObjectReader = {
    readerFor(classFor[T])
  }

  /**
   * Factory method for constructing [[com.fasterxml.jackson.databind.ObjectReader]] that will
   * deserialize objects using specified JSON View (filter).
   */
  def readerWithView[T: ClassTag]: ObjectReader = {
    readerWithView(classFor[T])
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
  def convertValue[T: ClassTag](fromValue: Any): T = {
    convertValue(fromValue, classFor[T])
  }

  private def classFor[T: ClassTag]: Class[T] = {
    implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]]
  }
}
