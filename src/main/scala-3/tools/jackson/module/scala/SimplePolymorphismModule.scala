package tools.jackson.module.scala

import tools.jackson.databind.JacksonModule.SetupContext
import tools.jackson.module.scala.deser.SimplePolymorphismDeserializerModule
import tools.jackson.module.scala.ser.SimplePolymorphismSerializerModule
import tools.jackson.module.scala.util.SimplePolymorphism

/**
 * Automatic polymorphic support for hierarchies marked with [[SimplePolymorphismSupport]].
 *
 * @since 3.3.0
 */
trait SimplePolymorphismModule extends SimplePolymorphismSerializerModule with SimplePolymorphismDeserializerModule {
  override def getModuleName: String = "SimplePolymorphismModule"

  override def getInitializers(config: ScalaModule.Config): Seq[SetupContext => Unit] = {
    SimplePolymorphismSerializerModule.getInitializers(config) ++
      SimplePolymorphismDeserializerModule.getInitializers(config)
  }
}

object SimplePolymorphismModule extends SimplePolymorphismModule {

  /**
   * Replaces the [[LookupCacheFactory]] used for the cache of resolved `@type` names. The default
   * factory uses [[tools.jackson.databind.util.SimpleLookupCache]].
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
    SimplePolymorphism.setLookupCacheFactory(lookupCacheFactory)

  /**
   * Resize the cache of resolved `@type` names. The default size is 1000.
   *
   * @param size new size for the cache
   * @see [[setLookupCacheFactory]]
   * @since 3.3.0
   */
  def setSubtypeCacheSize(size: Int): Unit = SimplePolymorphism.setCacheSize(size)

  /**
   * Empties the cache of resolved `@type` names.
   *
   * @since 3.3.0
   */
  def clearSubtypeCache(): Unit = SimplePolymorphism.clearCache()
}
