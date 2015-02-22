package com.fasterxml.jackson.module.scala.modifiers

import com.fasterxml.jackson.module.scala.JacksonModule

private object OptionTypeModifier extends CollectionLikeTypeModifier {
  def BASE = classOf[Option[Any]]
}

trait OptionTypeModifierModule extends JacksonModule {
  this += OptionTypeModifier
}