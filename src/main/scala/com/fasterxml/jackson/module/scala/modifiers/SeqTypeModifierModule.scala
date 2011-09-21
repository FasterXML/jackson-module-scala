package com.fasterxml.jackson.module.scala.modifiers

import com.fasterxml.jackson.module.scala.JacksonModule

private object SeqTypeModifier extends CollectionLikeTypeModifier {
  val BASE = classOf[Seq[Any]]
}

trait SeqTypeModifierModule {
  self: JacksonModule =>

  this += SeqTypeModifier
}