package tools.jackson.module.scala.poly

import tools.jackson.module.scala.SealedPolymorphismSupport

// not sealed, so an implementation may be declared anywhere on the classpath
trait Unsealed extends SealedPolymorphismSupport
case class Nearby(x: Int) extends Unsealed

case class UnsealedHolder(u: Unsealed)

// two implementations whose derived names collide - one in the root's companion, one beside it
sealed trait Shadow extends SealedPolymorphismSupport
object Shadow {
  case class Entry(a: Int) extends Shadow
}
case class Entry(b: String) extends Shadow

case class ShadowHolder(s: Shadow)
