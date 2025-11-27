package tools.jackson.module.scala.`enum`

import com.fasterxml.jackson.annotation.JsonProperty

enum ColorEnum { case Red, Green, Blue }

case class Colors(set: Set[ColorEnum])
