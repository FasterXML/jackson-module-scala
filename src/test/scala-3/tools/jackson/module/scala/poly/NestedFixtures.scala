package tools.jackson.module.scala.poly

import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import tools.jackson.module.scala.SimplePolymorphismSupport

// the inner hierarchy, marked
sealed trait Inner extends SimplePolymorphismSupport
case class InnerA(a: Int) extends Inner
case object InnerB extends Inner

// both hierarchies marked - a polymorphic value holding a polymorphic value
sealed trait Outer extends SimplePolymorphismSupport
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
sealed trait AnnOuter extends SimplePolymorphismSupport
case class AnnOuterA(inner: Inner) extends AnnOuter
case class AnnOuterB(label: String) extends AnnOuter

case class AnnOuterHolder(outer: AnnOuter)

// and the other way round - Jackson owns the inner hierarchy, we own the outer one
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes(Array(
  new JsonSubTypes.Type(value = classOf[AnnInnerA], name = "AnnInnerA"),
  new JsonSubTypes.Type(value = classOf[AnnInnerB], name = "AnnInnerB")
))
sealed trait AnnInner extends SimplePolymorphismSupport
case class AnnInnerA(a: Int) extends AnnInner
case class AnnInnerB(b: String) extends AnnInner

sealed trait PlainOuter extends SimplePolymorphismSupport
case class PlainOuterA(inner: AnnInner) extends PlainOuter
case object PlainOuterB extends PlainOuter

case class PlainOuterHolder(outer: PlainOuter)
