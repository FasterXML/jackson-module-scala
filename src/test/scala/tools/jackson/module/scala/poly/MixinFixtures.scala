package tools.jackson.module.scala.poly

// a sealed hierarchy whose classes cannot be changed - no marker trait anywhere on it
sealed trait Vehicle
case class Car(wheels: Int) extends Vehicle
case class Truck(axles: Int, load: String) extends Vehicle
case object Bike extends Vehicle
// an implementation below an intermediate trait, so the mix-in is two levels above it
sealed trait Powered extends Vehicle
case class Van(seats: Int) extends Powered

case class Garage(vehicle: Vehicle)
