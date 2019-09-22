package com.fasterxml.jackson.module.scala.experimental

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.cfg.MapperBuilder

/**
 * @tparam M
 * @tparam B
 * @since 3.0.0
 */
trait ScalaMapperBuilder[M <: ObjectMapper,B <: MapperBuilder[M,B]] {
  self: MapperBuilder[M,B] =>

  /**
   * Method to use for adding mix-in annotations to use for augmenting
   * specified class or interface. All annotations from
   * <code>mixinSource</code> are taken to override annotations
   * that <code>target</code> (or its supertypes) has.
   *
   * @tparam Target Class (or interface) whose annotations to effectively override
   * @tparam MixinSource Class (or interface) whose annotations are to
   *                     be "added" to target's annotations, overriding as necessary
   */
  final def addMixin[Target: Manifest, MixinSource: Manifest](): B = {
    addMixIn(manifest[Target].runtimeClass, manifest[MixinSource].runtimeClass)
  }

  final def findMixInClassFor[T: Manifest]: Class[_] = {
    mixInHandler()findMixInClassFor(manifest[T].runtimeClass)
  }
}
