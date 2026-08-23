package tools.jackson.module.scala.poly.mixin

import tools.jackson.module.scala.SealedPolymorphismSupport

/**
 * What the owner of the mapper supplies for a hierarchy they cannot change, in their own file and
 * package - which is the whole point.
 *
 * It carries the marker and nothing else. It deliberately does not extend the hierarchy's base:
 * `extends Vehicle with SealedPolymorphismSupport` is rejected as "illegal inheritance from sealed
 * trait Vehicle" anywhere but the file the hierarchy is declared in, and Jackson never requires a
 * mix-in to be a subtype of what it is mixed into - it only harvests from it.
 */
trait VehicleMixin extends SealedPolymorphismSupport
