# Overview

Project to build Jackson (http://jackson.codehaus.org) module (jar) that supports Scala (http://www.scala-lang.org/) data types.

The Scala Module supports serialization and limited deserialization of Scala Lists, Maps, and other immutable and mutable collection types. Scala's collection API does not inheret from Java's collection API; therefore this module is necessary to support Scala.

# Usage

To use the Scala Module in Jackson, simply register it with the ObjectMapper instance:

    val mapper = new ObjectMapper()
    mapper.registerModule(new ScalaModule())

ObjectMapper methods readValue and writeValue will use the ScalaModule's ScalaDeserializers and ScalaSerializers to implement the Scala collection conversions. 

For more documentation on usage, please refer to the scala test cases:

* src/test/scala/com/fasterxml/jackson/module/scala/SerializationTest.scala
* src/test/scala/com/fasterxml/jackson/module/scala/DeserializationTest.scala
