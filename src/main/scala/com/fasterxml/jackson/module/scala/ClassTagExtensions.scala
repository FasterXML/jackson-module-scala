package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.core.{JsonParser, TreeNode}
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.`type`.{MapLikeType, TypeFactory}
import com.fasterxml.jackson.databind.json.JsonMapper

import java.io.{File, InputStream, Reader}
import java.net.URL
import scala.collection.{immutable, mutable}
import scala.collection.immutable.IntMap
import scala.reflect.ClassTag

object ClassTagExtensions {
  def ::(o: JsonMapper): JsonMapper with ClassTagExtensions = new Mixin(o)
  def ::(o: ObjectMapper): ObjectMapper with ClassTagExtensions = new ObjectMapperMixin(o)

  final class Mixin private[ClassTagExtensions](mapper: JsonMapper)
    extends JsonMapper(mapper) with ClassTagExtensions

  final class ObjectMapperMixin private[ClassTagExtensions](mapper: ObjectMapper)
    extends ObjectMapper(mapper) with ClassTagExtensions
}

/**
 * ClassTag equivalent of [[ScalaObjectMapper]] (which it is meant to replace).
 * This only works with non parameterized types or parameterized types up to 5 type parameters.
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

  /**
   * Convenience method for constructing [[com.fasterxml.jackson.databind.JavaType]] out of given
   * type (typically <code>java.lang.Class</code>), but without explicit
   * context.
   */
  def constructType[T: JavaTypeable]: JavaType = {
    implicitly[JavaTypeable[T]].asJavaType(getTypeFactory)
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
  def readValue[T: JavaTypeable](jp: JsonParser): T = {
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
  def readValues[T: JavaTypeable](jp: JsonParser): MappingIterator[T] = {
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
  def treeToValue[T: JavaTypeable](n: TreeNode): T = {
    treeToValue(n, constructType[T])
  }

  /*
   **********************************************************
   * Extended Public API, deserialization,
   * convenience methods
   **********************************************************
   */
  def readValue[T: JavaTypeable](src: File): T = {
    readValue(src, constructType[T])
  }

  def readValue[T: JavaTypeable](src: URL): T = {
    readValue(src, constructType[T])
  }

  def readValue[T: JavaTypeable](content: String): T = {
    readValue(content, constructType[T])
  }

  def readValue[T: JavaTypeable](src: Reader): T = {
    readValue(src, constructType[T])
  }

  def readValue[T: JavaTypeable](src: InputStream): T = {
    readValue(src, constructType[T])
  }

  def readValue[T: JavaTypeable](src: Array[Byte]): T = {
    readValue(src, constructType[T])
  }

  def readValue[T: JavaTypeable](src: Array[Byte], offset: Int, len: Int): T = {
    readValue(src, offset, len, constructType[T])
  }

  def updateValue[T: JavaTypeable](valueToUpdate: T, src: File): T = {
    objectReaderFor(valueToUpdate).readValue(src)
  }

  def updateValue[T: JavaTypeable](valueToUpdate: T, src: URL): T = {
    objectReaderFor(valueToUpdate).readValue(src)
  }

  def updateValue[T: JavaTypeable](valueToUpdate: T, content: String): T = {
    objectReaderFor(valueToUpdate).readValue(content)
  }

  def updateValue[T: JavaTypeable](valueToUpdate: T, src: Reader): T = {
    objectReaderFor(valueToUpdate).readValue(src)
  }

  def updateValue[T: JavaTypeable](valueToUpdate: T, src: InputStream): T = {
    objectReaderFor(valueToUpdate).readValue(src)
  }

  def updateValue[T: JavaTypeable](valueToUpdate: T, src: Array[Byte]): T = {
    objectReaderFor(valueToUpdate).readValue(src)
  }

  def updateValue[T: JavaTypeable](valueToUpdate: T, src: Array[Byte], offset: Int, len: Int): T = {
    objectReaderFor(valueToUpdate).readValue(src, offset, len)
  }

  private def objectReaderFor[T: JavaTypeable](valueToUpdate: T): ObjectReader = {
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
  def writerWithView[T: ClassTag]: ObjectWriter = {
    writerWithView(classFor[T])
  }

  /**
   * Factory method for constructing [[com.fasterxml.jackson.databind.ObjectWriter]] that will
   * serialize objects using specified root type, instead of actual
   * runtime type of value. Type must be a super-type of runtime type.
   * <p>
   * Main reason for using this method is performance, as writer is able
   * to pre-fetch serializer to use before write, and if writer is used
   * more than once this avoids addition per-value serializer lookups.
   */
  def writerFor[T: JavaTypeable]: ObjectWriter = {
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
  def readerFor[T: JavaTypeable]: ObjectReader = {
    readerFor(constructType[T])
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
   * @throws java.lang.IllegalArgumentException If conversion fails due to incompatible type;
   *                                            if so, root cause will contain underlying checked exception data
   *                                            binding functionality threw
   */
  def convertValue[T: JavaTypeable](fromValue: Any): T = {
    convertValue(fromValue, constructType[T])
  }

  private def classFor[T: ClassTag]: Class[T] = {
    implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]]
  }
}

trait JavaTypeable[T] {
  def asJavaType(typeFactory: TypeFactory): JavaType
}

object JavaTypeable {

  private val intClass = classOf[Int]
  private val intMapClass = classOf[IntMap[_]]
  private val longClass = classOf[Long]
  private val immutableLongMapClass = classOf[immutable.LongMap[_]]
  private val mutableLongMapClass = classOf[mutable.LongMap[_]]

  // order of implicits matters for performance reasons, place most useful implicits last

  implicit def gen5JavaTypeable[T[_, _, _, _, _], A: JavaTypeable, B: JavaTypeable, C: JavaTypeable, D: JavaTypeable, E: JavaTypeable](implicit ct: ClassTag[T[A, B, C, D, E]]): JavaTypeable[T[A, B, C, D, E]] = {
    new JavaTypeable[T[A, B, C, D, E]] {
      override def asJavaType(typeFactory: TypeFactory): JavaType = {
        val typeArgs: Array[JavaType] = Array(
          implicitly[JavaTypeable[A]].asJavaType(typeFactory),
          implicitly[JavaTypeable[B]].asJavaType(typeFactory),
          implicitly[JavaTypeable[C]].asJavaType(typeFactory),
          implicitly[JavaTypeable[D]].asJavaType(typeFactory),
          implicitly[JavaTypeable[E]].asJavaType(typeFactory)
        )
        typeFactory.constructParametricType(ct.runtimeClass, typeArgs: _*)
      }
    }
  }

  implicit def gen4JavaTypeable[T[_, _, _, _], A: JavaTypeable, B: JavaTypeable, C: JavaTypeable, D: JavaTypeable](implicit ct: ClassTag[T[A, B, C, D]]): JavaTypeable[T[A, B, C, D]] = {
    new JavaTypeable[T[A, B, C, D]] {
      override def asJavaType(typeFactory: TypeFactory): JavaType = {
        val typeArgs: Array[JavaType] = Array(
          implicitly[JavaTypeable[A]].asJavaType(typeFactory),
          implicitly[JavaTypeable[B]].asJavaType(typeFactory),
          implicitly[JavaTypeable[C]].asJavaType(typeFactory),
          implicitly[JavaTypeable[D]].asJavaType(typeFactory)
        )
        typeFactory.constructParametricType(ct.runtimeClass, typeArgs: _*)
      }
    }
  }

  implicit def gen3JavaTypeable[T[_, _, _], A: JavaTypeable, B: JavaTypeable, C: JavaTypeable](implicit ct: ClassTag[T[A, B, C]]): JavaTypeable[T[A, B, C]] = {
    new JavaTypeable[T[A, B, C]] {
      override def asJavaType(typeFactory: TypeFactory): JavaType = {
        val typeArgs: Array[JavaType] = Array(
          implicitly[JavaTypeable[A]].asJavaType(typeFactory),
          implicitly[JavaTypeable[B]].asJavaType(typeFactory),
          implicitly[JavaTypeable[C]].asJavaType(typeFactory)
        )
        typeFactory.constructParametricType(ct.runtimeClass, typeArgs: _*)
      }
    }
  }

  implicit def gen2JavaTypeable[T[_, _], A: JavaTypeable, B: JavaTypeable](implicit ct: ClassTag[T[A, B]]): JavaTypeable[T[A, B]] = {
    new JavaTypeable[T[A, B]] {
      override def asJavaType(typeFactory: TypeFactory): JavaType = {
        val typeArgs: Array[JavaType] = Array(
          implicitly[JavaTypeable[A]].asJavaType(typeFactory),
          implicitly[JavaTypeable[B]].asJavaType(typeFactory)
        )
        typeFactory.constructParametricType(ct.runtimeClass, typeArgs: _*)
      }
    }
  }

  implicit def gen1JavaTypeable[T[_], A: JavaTypeable](implicit ct: ClassTag[T[A]]): JavaTypeable[T[A]] = {
    new JavaTypeable[T[A]] {
      override def asJavaType(typeFactory: TypeFactory): JavaType = {
        val typeArgs: Array[JavaType] = Array(
          implicitly[JavaTypeable[A]].asJavaType(typeFactory)
        )
        typeFactory.constructParametricType(ct.runtimeClass, typeArgs: _*)
      }
    }
  }

  implicit def gen0JavaTypeable[T](implicit ct: ClassTag[T]): JavaTypeable[T] = {
    new JavaTypeable[T] {
      override def asJavaType(typeFactory: TypeFactory): JavaType = {
        val typeArgs: Array[JavaType] = Array()
        typeFactory.constructParametricType(ct.runtimeClass, typeArgs: _*)
      }
    }
  }

  implicit def arrayJavaTypeable[T : JavaTypeable]: JavaTypeable[Array[T]] = {
    new JavaTypeable[Array[T]] {
      override def asJavaType(typeFactory: TypeFactory): JavaType = {
        val typeArg0 = implicitly[JavaTypeable[T]].asJavaType(typeFactory)
        typeFactory.constructArrayType(typeArg0)
      }
    }
  }

  implicit def mapJavaTypeable[M[_,_] <: Map[_,_], K : JavaTypeable, V: JavaTypeable](implicit ct: ClassTag[M[K,V]]): JavaTypeable[M[K, V]] = {
    new JavaTypeable[M[K, V]] {
      override def asJavaType(typeFactory: TypeFactory): JavaType = {
        val typeArg0 = implicitly[JavaTypeable[K]].asJavaType(typeFactory)
        val typeArg1 = implicitly[JavaTypeable[V]].asJavaType(typeFactory)
        typeFactory.constructMapLikeType(ct.runtimeClass, typeArg0, typeArg1)
      }
    }
  }

  implicit def collectionJavaTypeable[I[_] <: Iterable[_], T : JavaTypeable](implicit ct: ClassTag[I[T]]): JavaTypeable[I[T]] = {
    new JavaTypeable[I[T]] {
      override def asJavaType(typeFactory: TypeFactory): JavaType = {
        val typeArg0 = implicitly[JavaTypeable[T]].asJavaType(typeFactory)
        if (intMapClass.isAssignableFrom(ct.runtimeClass)) {
          MapLikeType.upgradeFrom(
            typeFactory.constructType(intMapClass),
            typeFactory.constructType(intClass),
            typeArg0
          )
        } else if (immutableLongMapClass.isAssignableFrom(ct.runtimeClass)) {
          MapLikeType.upgradeFrom(
            typeFactory.constructType(immutableLongMapClass),
            typeFactory.constructType(longClass),
            typeArg0
          )
        } else if (mutableLongMapClass.isAssignableFrom(ct.runtimeClass)) {
          MapLikeType.upgradeFrom(
            typeFactory.constructType(mutableLongMapClass),
            typeFactory.constructType(longClass),
            typeArg0
          )
        } else {
          typeFactory.constructCollectionLikeType(ct.runtimeClass, typeArg0)
        }
      }
    }
  }

  implicit def optionJavaTypeable[T : JavaTypeable]: JavaTypeable[Option[T]] = {
    new JavaTypeable[Option[T]] {
      override def asJavaType(typeFactory: TypeFactory): JavaType = {
        val typeArg0 = implicitly[JavaTypeable[T]].asJavaType(typeFactory)
        typeFactory.constructReferenceType(classOf[Option[_]], typeArg0)
      }
    }
  }

  implicit val anyJavaTypeable: JavaTypeable[Any] = {
    new JavaTypeable[Any] {
      override def asJavaType(typeFactory: TypeFactory): JavaType = {
        val typeArgs: Array[JavaType] = Array()
        typeFactory.constructParametricType(classOf[Object], typeArgs: _*)
      }
    }
  }

}
