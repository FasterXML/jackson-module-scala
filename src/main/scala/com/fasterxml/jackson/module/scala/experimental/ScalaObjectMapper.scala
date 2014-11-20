package com.fasterxml.jackson.module.scala.experimental

import java.io._
import java.net.URL

import com.fasterxml.jackson.core._
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper
import com.fasterxml.jackson.databind.jsonschema.JsonSchema
import com.fasterxml.jackson.module.scala.util.Implicits._
import com.google.common.cache.{CacheBuilder, LoadingCache}

import scala.language.existentials
import scala.reflect.api.Symbols
import scala.reflect.{ClassTag, classTag}
import scala.reflect.runtime.currentMirror
import scala.reflect.runtime.{universe => ru}
import ru.TypeRefTag // not unused, required for patmat

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
  final def addMixin[Target: ClassTag, MixinSource: ClassTag]() = {
    addMixIn(classTag[Target].runtimeClass, classTag[MixinSource].runtimeClass)
  }

  /**
   * @deprecated Since 2.5: replaced by a fluent form of the method; { @link #addMixIn(Class, Class)}.
   */
  final def addMixInAnnotations[Target: ClassTag, MixinSource: ClassTag]() = {
    addMixIn(classTag[Target].runtimeClass, classTag[MixinSource].runtimeClass)
  }

  final def findMixInClassFor[T: ClassTag]: Class[_] = {
    findMixInClassFor(classTag[T].runtimeClass)
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
  private[this] val typeCache: LoadingCache[ru.Type, JavaType] =
    CacheBuilder.newBuilder().maximumSize(DEFAULT_CACHE_SIZE).build(constructType2 _)

  private def constructType2(t: ru.Type): JavaType = {
    t match {
      case ru.TypeRef(_, sym, args) =>
        val clazz = if (isAnyRef(t)) {
          currentMirror.runtimeClass(t)
        } else {
          boxPrimitive(sym)
        }
        val typeArguments: Seq[JavaType] = args.map(typeCache.get)
        if (isArray(t)) {
          getTypeFactory.constructArrayType(typeArguments(0))
        } else if (isMapLike(t)) {
          getTypeFactory.constructMapLikeType(clazz, typeArguments(0), typeArguments(1))
        } else if (isCollectionLike(t)) {
          getTypeFactory.constructCollectionLikeType(clazz, typeArguments(0))
        } else {
          getTypeFactory.constructParametrizedType(clazz, clazz, typeArguments.toArray: _*)
        }
    }
  }

  def constructType[T](implicit t: ru.TypeTag[T]): JavaType = typeCache.get(t.tpe)

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
  def readValue[T: ru.TypeTag](jp: JsonParser): T = {
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
  def readValues[T: ru.TypeTag](jp: JsonParser): MappingIterator[T] = {
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
  def treeToValue[T: ClassTag](n: TreeNode): T = {
    treeToValue(n, classTag[T].runtimeClass).asInstanceOf[T]
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
   */
  def canSerialize[T: ClassTag]: Boolean = {
    canSerialize(classTag[T].runtimeClass)
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
   */
  def canDeserialize[T: ru.TypeTag]: Boolean = {
    canDeserialize(constructType[T])
  }

  /*
   **********************************************************
   * Extended Public API, deserialization,
   * convenience methods
   **********************************************************
   */
  def readValue[T: ru.TypeTag](src: File): T = {
    readValue(src, constructType[T])
  }

  def readValue[T: ru.TypeTag](src: URL): T = {
    readValue(src, constructType[T])
  }

  def readValue[T: ru.TypeTag](content: String): T = {
    readValue(content, constructType[T])
  }

  def readValue[T: ru.TypeTag](src: Reader): T = {
    readValue(src, constructType[T])
  }

  def readValue[T: ru.TypeTag](src: InputStream): T = {
    readValue(src, constructType[T])
  }

  def readValue[T: ru.TypeTag](src: Array[Byte]): T = {
    readValue(src, constructType[T])
  }

  def readValue[T: ru.TypeTag](src: Array[Byte], offset: Int, len: Int): T = {
    readValue(src, offset, len, constructType[T])
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
    writerWithView(classTag[T].runtimeClass)
  }

  /**
   * @deprecated Since 2.5, use { @link #writerFor(Class)} instead
   */
  def writerWithType[T: ru.TypeTag]: ObjectWriter = {
    writerFor[T]
  }

  /**
   * Factory method for constructing {@link ObjectWriter} that will
   * serialize objects using specified root type, instead of actual
   * runtime type of value. Type must be a super-type of runtime type.
   * <p>
   * Main reason for using this method is performance, as writer is able
   * to pre-fetch serializer to use before write, and if writer is used
   * more than once this avoids addition per-value serializer lookups.
   *
   * @since 2.5
   */
  def writerFor[T: ru.TypeTag]: ObjectWriter = {
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
  def reader[T: ru.TypeTag]: ObjectReader = {
    reader(constructType[T])
  }

  /**
   * Factory method for constructing [[com.fasterxml.jackson.databind.ObjectReader]] that will
   * deserialize objects using specified JSON View (filter).
   */
  def readerWithView[T: ClassTag]: ObjectReader = {
    readerWithView(classTag[T].runtimeClass)
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
  def convertValue[T: ru.TypeTag](fromValue: Any): T = {
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
  def generateJsonSchema[T: ClassTag]: JsonSchema = {
    generateJsonSchema(classTag[T].runtimeClass)
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
  def acceptJsonFormatVisitor[T: ru.TypeTag](visitor: JsonFormatVisitorWrapper) {
    acceptJsonFormatVisitor(constructType[T], visitor)
  }

  private val ANYREF = ru.typeOf[AnyRef]
  private def isAnyRef(t: ru.Type): Boolean = t <:< ANYREF

  private val ARRAY = ru.typeOf[scala.Array[_]]
  private def isArray(t: ru.Type): Boolean = t <:< ARRAY

  private val MAP = ru.typeOf[collection.Map[_,_]]
  private def isMapLike(t: ru.Type): Boolean = t <:< MAP

  private val TRAVERSABLE = ru.typeOf[collection.Traversable[_]]
  private val OPTION = ru.typeOf[Option[_]]
  private def isCollectionLike(t: ru.Type): Boolean = t <:< TRAVERSABLE || t <:< OPTION

  // The cake is a lie
  private val DoubleClass: Symbols # SymbolApi = ru.definitions.DoubleClass
  private val FloatClass: Symbols # SymbolApi = ru.definitions.FloatClass
  private val LongClass: Symbols # SymbolApi = ru.definitions.LongClass
  private val IntClass: Symbols # SymbolApi = ru.definitions.IntClass
  private val CharClass: Symbols # SymbolApi = ru.definitions.CharClass
  private val ShortClass: Symbols # SymbolApi = ru.definitions.ShortClass
  private val ByteClass: Symbols # SymbolApi = ru.definitions.ByteClass
  private val BooleanClass: Symbols # SymbolApi = ru.definitions.BooleanClass

  private def boxPrimitive(sym: Symbols # SymbolApi) = {
    sym match {
      case DoubleClass => classOf[Double]
      case FloatClass => classOf[Float]
      case LongClass => classOf[Long]
      case IntClass => classOf[Int]
      case CharClass => classOf[Char]
      case ShortClass => classOf[Short]
      case ByteClass => classOf[Byte]
      case BooleanClass => classOf[Boolean]
      case _ => classOf[AnyRef]
    }
  }
}