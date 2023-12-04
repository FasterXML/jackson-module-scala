package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import scala.collection.immutable

class DefaultTypingMapDeserializerTest extends DeserializerTest {

  def module: DefaultScalaModule.type = DefaultScalaModule

  override def newMapper: ObjectMapper = {
    val mapper = super.newMapper
    mapper.activateDefaultTyping(mapper.getPolymorphicTypeValidator)
  }

  "Scala Module" should "deserialize immutable Map when default typing enabled" in {
    val map = HasMap(immutable.Map("one" -> "one", "two" -> "two"))

    val mapper = newMapper

    val json = mapper.writeValueAsString(map)
    // Was failing in Scala 2.13+ with:
    // > Could not resolve type id 'scala.collection.convert.JavaCollectionWrappers$MapWrapper' as a subtype of
    // > `scala.collection.immutable.Map<java.lang.String,java.lang.String>`: Not a subtype
    //
    // prior the changing MapSerializerModule.scala to use an inner class for MapWrapper
    val read = mapper.readValue(json, classOf[HasMap])

    read shouldEqual map
  }

}

case class HasMap(m: Map[String, String])
