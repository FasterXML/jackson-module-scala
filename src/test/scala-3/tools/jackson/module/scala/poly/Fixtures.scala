package tools.jackson.module.scala.poly

import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import tools.jackson.module.scala.SimplePolymorphismSupport

// implementations declared alongside the base type
sealed trait Animal extends SimplePolymorphismSupport
case class Dog(name: String) extends Animal
case class Bird(name: String, canFly: Boolean) extends Animal
case object Unknown extends Animal

case class Owner(name: String, pet: Animal)
case class MaybeOwner(name: String, pet: Option[Animal])
case class Zoo(animals: Map[String, Animal])
case class Shelter(animals: Seq[Animal])

// a sealed abstract class base, with state of its own
sealed abstract class Shape(val sides: Int) extends SimplePolymorphismSupport
case class Rect(width: Double, height: Double) extends Shape(4)
case object Point extends Shape(0)

case class Drawing(shape: Shape)

// implementations declared inside the companion of the base type
sealed trait Payment extends SimplePolymorphismSupport
object Payment {
  case class Card(last4: String) extends Payment
  case object Cash extends Payment
}

case class Order(total: Int, payment: Payment)

// base type and implementations both nested inside an unrelated object
object Wrapper {
  sealed trait Event extends SimplePolymorphismSupport
  case class Created(id: Int) extends Event
  case object Deleted extends Event
}

case class Envelope(event: Wrapper.Event)

// a marked hierarchy that also carries Jackson's own annotations - they win
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes(Array(new JsonSubTypes.Type(value = classOf[Cheque], name = "Cheque")))
sealed trait Annotated extends SimplePolymorphismSupport
case class Cheque(number: Int) extends Annotated

case class Ledger(entry: Annotated)

// a hierarchy that is not marked - it must be untouched
sealed trait Plain
case class PlainDog(name: String) extends Plain
