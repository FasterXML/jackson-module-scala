package com.fasterxml.jackson.module.scala

object Wrapper {
  object Foo {
    class Baz(val num: Int)
  }
}
class Qux(val qux: Wrapper.Foo.Baz)
