package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.cfg.MapperBuilder

import scala.reflect.ClassTag

/**
 * @tparam M
 * @tparam B
 * @since 2.13.0
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
  final def addMixin[Target: ClassTag, MixinSource: ClassTag](): B = {
    addMixIn(implicitly[ClassTag[Target]].runtimeClass, implicitly[ClassTag[MixinSource]].runtimeClass)
  }
}
