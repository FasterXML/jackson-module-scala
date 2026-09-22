package tools.jackson.module.scala.poly.sharedroot

import tools.jackson.module.scala.SealedPolymorphismSupport

// A mix-in marks a hierarchy for one mapper without changing the classes, so which type is the
// root of a marked hierarchy differs from mapper to mapper.
trait TopMixin extends SealedPolymorphismSupport
trait InnerMixin extends SealedPolymorphismSupport
