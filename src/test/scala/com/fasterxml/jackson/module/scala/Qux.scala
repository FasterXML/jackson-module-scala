package com.fasterxml.jackson.module.scala

object FooBazWrapper {
  object Foo {
    class Baz(val num: Int)
  }
}
class Qux(val qux: FooBazWrapper.Foo.Baz)
