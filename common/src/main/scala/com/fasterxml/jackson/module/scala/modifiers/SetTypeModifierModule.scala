package com.fasterxml.jackson.module.scala.modifiers

import com.fasterxml.jackson.module.scala.JacksonModule

private object SetTypeModifier extends CollectionLikeTypeModifier {
  val BASE = classOf[collection.Set[Any]]
}

trait SetTypeModifierModule extends JacksonModule {
  this += SetTypeModifier
}
