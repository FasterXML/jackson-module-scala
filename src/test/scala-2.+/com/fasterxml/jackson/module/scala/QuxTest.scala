package com.fasterxml.jackson.module.scala

import org.scalatest.funsuite.AnyFunSuite
import com.fasterxml.jackson.databind.json.JsonMapper

object Wrapper {
  object Foo {
    class Baz(val num: Int)
  }
}
class Qux(val qux: Wrapper.Foo.Baz)

class QuxTest extends AnyFunSuite {
  test("Quxin") {
    val objectMapper =
      (JsonMapper
        .builder().addModule(
        DefaultScalaModule).build() :: ScalaObjectMapper)
    val qux = objectMapper.readValue[Qux]("""{"qux": {"num": "3"}}""")
    assert(qux.qux.num == 3)
  }
}
