package tools.jackson.module.scala.poly.sharedroot.first

// One of two identical copies, each in a package of its own, so that both directions of the leak
// can be shown without one test's cache entries deciding the other's outcome.
//
// `Shared` is the derived `@type` name of the top-level implementation relative to `Top`, and also
// of the nested one relative to `Box.Inner`. The same (base class, name) pair therefore names two
// different classes depending on which type is the root of the hierarchy. Neither type extends the
// marker: a mix-in marks one or the other, which is what makes the root a property of the mapper.

sealed trait Top
case class Shared(v: Int) extends Top

object Box {
  sealed trait Inner extends Top
  case class Shared(v: String) extends Inner
}
