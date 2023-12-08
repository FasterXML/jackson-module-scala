package com.fasterxml.jackson.module.scala.`enum`.adt

enum Color(val rgb: Int):
  case Red   extends Color(0xFF0000)
  case Green extends Color(0x00FF00)
  case Blue  extends Color(0x0000FF)
  case Mix(mix: Int) extends Color(mix)

case class ColorSet(set: Set[Color])  
