package com.fasterxml.jackson.module.scala.experimental

import java.io.{File, InputStream, Reader}
import java.net.URL

import com.fasterxml.jackson.databind.jsonschema.JsonSchema
import com.fasterxml.jackson.core.{JsonParser, TreeNode}
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper
import com.fasterxml.jackson.databind.{MappingIterator, ObjectMapper, ObjectReader, ObjectWriter}

/**
 * @deprecated use {@link com.fasterxml.jackson.module.scala.ScalaObjectMapper}
 */
@Deprecated
trait ScalaObjectMapper extends com.fasterxml.jackson.module.scala.ScalaObjectMapper {
  self: ObjectMapper =>

  //  This is to tell scala compiler to create ScalaObjectMapper.$init$
  assert(true)

  //  Methods above are overwritten just to maintain binary compatibility for trait implementations
  //  By default it's implemented via default interface methods so we need to

  override def readValue[T: Manifest](jp: JsonParser): T = {
    super.readValue(jp)
  }

  override def readValues[T: Manifest](jp: JsonParser): MappingIterator[T] = {
    super.readValues[T](jp)
  }

  override def treeToValue[T: Manifest](n: TreeNode): T = {
    super.treeToValue[T](n)
  }

  override def canSerialize[T: Manifest]: Boolean = {
    super.canSerialize[T]
  }

  override def canDeserialize[T: Manifest]: Boolean = {
    super.canDeserialize[T]
  }

  override def readValue[T: Manifest](src: File): T = {
    super.readValue[T](src)
  }

  override def readValue[T: Manifest](src: URL): T = {
    super.readValue[T](src)
  }

  override def readValue[T: Manifest](content: String): T = {
    super.readValue[T](content)
  }

  override def readValue[T: Manifest](src: Reader): T = {
    super.readValue[T](src)
  }

  override def readValue[T: Manifest](src: InputStream): T = {
    super.readValue[T](src)
  }

  override def readValue[T: Manifest](src: Array[Byte]): T = {
    super.readValue[T](src)
  }

  override def readValue[T: Manifest](src: Array[Byte], offset: Int, len: Int): T = {
    super.readValue[T](src, offset, len)
  }

  override def writerWithView[T: Manifest]: ObjectWriter = {
    super.writerWithView[T]
  }

  /**
   * @deprecated Since 2.5, use { @link #writerFor(Class)} instead
   */
  override def writerWithType[T: Manifest]: ObjectWriter = {
    super.writerWithType[T]
  }

  override def writerFor[T: Manifest]: ObjectWriter = {
    super.writerFor[T]
  }

  @deprecated(message = "Replaced with readerFor", since = "2.6")
  override def reader[T: Manifest]: ObjectReader = {
    super.reader[T]
  }

  override def readerFor[T: Manifest]: ObjectReader = {
    super.readerFor[T]
  }

  override def readerWithView[T: Manifest]: ObjectReader = {
    super.readerWithView[T]
  }

  override def convertValue[T: Manifest](fromValue: Any): T = {
    super.convertValue[T](fromValue)
  }

  @deprecated("JsonSchema is deprecated in favor of JsonFormatVisitor", "2.2.2")
  override def generateJsonSchema[T: Manifest]: JsonSchema = {
    super.generateJsonSchema[T]
  }

  override def acceptJsonFormatVisitor[T: Manifest](visitor: JsonFormatVisitorWrapper): Unit = {
    super.acceptJsonFormatVisitor[T](visitor)
  }
}
