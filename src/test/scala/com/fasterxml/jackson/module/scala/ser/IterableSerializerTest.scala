package com.fasterxml.jackson.module.scala.ser

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.fasterxml.jackson.module.scala.JacksonModule
import collection.Iterator

/**
 * Undocumented class.
 */
@RunWith(classOf[JUnitRunner])
class IterableSerializerTest extends SerializerTest with FlatSpec with ShouldMatchers {

  lazy val module = new JacksonModule with IterableSerializerModule

  "An ObjectMapper with IterableSerializer" should "serialize an Iterable[Int]" in {
    val iterable = new Iterable[Int] {
          def iterator = Iterator(1, 2, 3)
      }

    serialize(iterable) should be ("[1,2,3]")
  }

}