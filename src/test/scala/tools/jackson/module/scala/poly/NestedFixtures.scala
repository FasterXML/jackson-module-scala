package tools.jackson.module.scala.poly

import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import tools.jackson.module.scala.SealedPolymorphismSupport

// the inner hierarchy, marked
sealed trait Inner extends SealedPolymorphismSupport
case class InnerA(a: Int) extends Inner
case object InnerB extends Inner

// both hierarchies marked - a polymorphic value holding a polymorphic value
sealed trait Outer extends SealedPolymorphismSupport
case class OuterA(inner: Inner) extends Outer
case object OuterB extends Outer
case class OuterC(inners: Seq[Inner]) extends Outer
// the hierarchy nests inside itself
case class OuterNest(next: Outer) extends Outer

case class NestHolder(outer: Outer)

// the outer hierarchy is annotated as well as marked, so Jackson owns it - the inner one is still ours
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes(Array(
  new JsonSubTypes.Type(value = classOf[AnnOuterA], name = "AnnOuterA"),
  new JsonSubTypes.Type(value = classOf[AnnOuterB], name = "AnnOuterB")
))
sealed trait AnnOuter extends SealedPolymorphismSupport
case class AnnOuterA(inner: Inner) extends AnnOuter
case class AnnOuterB(label: String) extends AnnOuter

case class AnnOuterHolder(outer: AnnOuter)

// and the other way round - Jackson owns the inner hierarchy, we own the outer one
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes(Array(
  new JsonSubTypes.Type(value = classOf[AnnInnerA], name = "AnnInnerA"),
  new JsonSubTypes.Type(value = classOf[AnnInnerB], name = "AnnInnerB")
))
sealed trait AnnInner extends SealedPolymorphismSupport
case class AnnInnerA(a: Int) extends AnnInner
case class AnnInnerB(b: String) extends AnnInner

sealed trait PlainOuter extends SealedPolymorphismSupport
case class PlainOuterA(inner: AnnInner) extends PlainOuter
case object PlainOuterB extends PlainOuter

case class PlainOuterHolder(outer: PlainOuter)

// an object nested inside another object, which unlike a top level object has no class carrying
// static forwarders - so the JVM reports it as the enclosing class by its module class, whose name
// ends in the `$` that separates it from what it encloses
object Deep {
  object Group {
    sealed trait Base extends SealedPolymorphismSupport
    case class Leaf(x: Int) extends Base
    case object Lone extends Base
  }

  // the base is declared in the outer object and the implementations in a nested one, so the name
  // of each keeps the nested object - a boundary that really does have to become a dot
  sealed trait Split extends SealedPolymorphismSupport
  object Held {
    case class Leaf(x: Int) extends Split
  }
}

case class DeepHolder(base: Deep.Group.Base)
case class SplitHolder(split: Deep.Split)
