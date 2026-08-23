package tools.jackson.module.scala.poly

// a sealed hierarchy whose classes cannot be changed - no marker trait anywhere on it
sealed trait Vehicle
case class Car(wheels: Int) extends Vehicle
case class Truck(axles: Int, load: String) extends Vehicle
case object Bike extends Vehicle

case class Garage(vehicle: Vehicle)
