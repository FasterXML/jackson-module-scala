package tools.jackson.module.scala.poly

import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import tools.jackson.module.scala.SealedPolymorphismSupport

// implementations declared alongside the base type
sealed trait Animal extends SealedPolymorphismSupport
case class Dog(name: String) extends Animal
case class Bird(name: String, canFly: Boolean) extends Animal
case object Unknown extends Animal

case class Owner(name: String, pet: Animal)
case class MaybeOwner(name: String, pet: Option[Animal])
case class Zoo(animals: Map[String, Animal])
case class Shelter(animals: Seq[Animal])

// a sealed abstract class base, with state of its own
sealed abstract class Shape(val sides: Int) extends SealedPolymorphismSupport
case class Rect(width: Double, height: Double) extends Shape(4)
case object Point extends Shape(0)

case class Drawing(shape: Shape)

// implementations declared inside the companion of the base type
sealed trait Payment extends SealedPolymorphismSupport
object Payment {
  case class Card(last4: String) extends Payment
  case object Cash extends Payment
}

case class Order(total: Int, payment: Payment)

// base type and implementations both nested inside an unrelated object
object Wrapper {
  sealed trait Event extends SealedPolymorphismSupport
  case class Created(id: Int) extends Event
  case object Deleted extends Event
}

case class Envelope(event: Wrapper.Event)

// a marked hierarchy that also carries Jackson's own annotations - they win
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes(Array(new JsonSubTypes.Type(value = classOf[Cheque], name = "Cheque")))
sealed trait Annotated extends SealedPolymorphismSupport
case class Cheque(number: Int) extends Annotated

case class Ledger(entry: Annotated)

// the annotation sits on one implementation rather than on the base - it governs that
// implementation's own subtypes and must not switch off tagging for the hierarchy
sealed trait Leafy extends SealedPolymorphismSupport
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
case class LeafA(x: Int) extends Leafy
case class LeafB(y: Int) extends Leafy

case class LeafHolder(leaf: Leafy)

// implementations grouped into objects that do not enclose the base - each keeps its enclosing
// object in the derived name, so two objects can hold an implementation of the same name
sealed trait Dup extends SealedPolymorphismSupport
object FirstGroup {
  case class Same(v: Int) extends Dup
  case object Only extends Dup
}
object SecondGroup { case class Same(v: String) extends Dup }

case class DupHolder(d: Dup)

// a concrete sealed class - a value in its own right, and a base its subclasses are read through
sealed class Node(val id: Int) extends SealedPolymorphismSupport
class Branch(id: Int, val label: String) extends Node(id)

case class Tree(node: Node)

// a hierarchy that is not marked - it must be untouched
sealed trait Plain
case class PlainDog(name: String) extends Plain
