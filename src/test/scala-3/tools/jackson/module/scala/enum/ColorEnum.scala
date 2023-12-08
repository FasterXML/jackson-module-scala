package tools.jackson.module.scala.`enum`

enum ColorEnum { case Red, Green, Blue }

case class Colors(set: Set[ColorEnum])
