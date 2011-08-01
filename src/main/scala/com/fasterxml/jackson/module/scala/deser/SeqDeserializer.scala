package com.fasterxml.jackson.module.scala.deser

import org.codehaus.jackson.JsonParser
import org.codehaus.jackson.`type`.JavaType
import org.codehaus.jackson.map.{DeserializationConfig, TypeDeserializer, DeserializationContext, JsonDeserializer}
import java.util.{Iterator, AbstractCollection, ArrayList}
import collection.mutable
import collection.immutable.Queue
import collection.generic.GenericCompanion
import org.codehaus.jackson.map.deser.{CollectionDeserializer, ContainerDeserializer}

