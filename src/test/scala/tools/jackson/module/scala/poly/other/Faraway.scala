package tools.jackson.module.scala.poly.other

import tools.jackson.module.scala.poly.Unsealed

// only reachable because Unsealed is not sealed - resolution can never find it from Unsealed
case class Faraway(x: Int) extends Unsealed
