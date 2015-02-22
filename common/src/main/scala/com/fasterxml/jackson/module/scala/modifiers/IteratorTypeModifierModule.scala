package com.fasterxml.jackson.module.scala.modifiers

import com.fasterxml.jackson.module.scala.JacksonModule

private object ScalaIteratorTypeModifier extends CollectionLikeTypeModifier {
  val BASE = classOf[Iterator[Any]]
}

trait IteratorTypeModifierModule extends JacksonModule {
  this += ScalaIteratorTypeModifier
}
