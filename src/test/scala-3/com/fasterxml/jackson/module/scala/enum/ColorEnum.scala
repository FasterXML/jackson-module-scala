package com.fasterxml.jackson.module.scala.`enum`

import com.fasterxml.jackson.annotation.JsonProperty

enum ColorEnum { case Red, Green, Blue }

enum AnnotatedColorEnum { @JsonProperty("red") Red, @JsonProperty("green") Green, @JsonProperty("blue") Blue }

case class Colors(set: Set[ColorEnum])
