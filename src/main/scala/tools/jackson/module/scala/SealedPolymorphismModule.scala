package tools.jackson.module.scala

import tools.jackson.databind.JacksonModule.SetupContext
import tools.jackson.module.scala.deser.SealedPolymorphismDeserializerModule
import tools.jackson.module.scala.ser.SealedPolymorphismSerializerModule
import tools.jackson.module.scala.util.SealedPolymorphism

/**
 * The state that automatic polymorphic support keeps, held per module instance rather than
 * globally, so that a `ScalaModule` built through [[ScalaModule.Builder]] has caches and cache
 * settings of its own. Mixing both halves of the module together yields one shared instance.
 *
 * @since 3.3.0
 */
trait SealedPolymorphismSupportState {

  private[scala] val sealedPolymorphism: SealedPolymorphism = new SealedPolymorphism

  /**
   * Replaces the [[LookupCacheFactory]] used by this module instance for the cache of resolved
   * `@type` names. The default factory uses [[tools.jackson.databind.util.SimpleLookupCache]].
   * <p>
   * Note that this clears the existing cache entries. The cache is only a memo of what can be
   * derived reflectively, so clearing it affects performance but not behaviour.
   * </p>
   *
   * @param lookupCacheFactory new factory
   * @see [[setSubtypeCacheSize]]
   * @since 3.3.0
   */
  def setLookupCacheFactory(lookupCacheFactory: LookupCacheFactory): Unit =
    sealedPolymorphism.setLookupCacheFactory(lookupCacheFactory)

  /**
   * Resize this module instance's cache of resolved `@type` names. The default size is 1000.
   *
   * @param size new size for the cache
   * @see [[setLookupCacheFactory]]
   * @since 3.3.0
   */
  def setSubtypeCacheSize(size: Int): Unit = sealedPolymorphism.setCacheSize(size)

  /**
   * Empties this module instance's cache of resolved `@type` names.
   *
   * @since 3.3.0
   */
  def clearSubtypeCache(): Unit = sealedPolymorphism.clearCache()
}

/**
 * Automatic polymorphic support for hierarchies marked with [[SealedPolymorphismSupport]].
 *
 * @since 3.3.0
 */
trait SealedPolymorphismModule extends SealedPolymorphismSerializerModule with SealedPolymorphismDeserializerModule {
  override def getModuleName: String = "SealedPolymorphismModule"

  // built from this instance's state, not from the two singleton halves
  override def getInitializers(config: ScalaModule.Config): Seq[SetupContext => Unit] =
    serializerInitializers(config) ++ deserializerInitializers(config)
}

object SealedPolymorphismModule extends SealedPolymorphismModule
