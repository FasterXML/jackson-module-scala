package tools.jackson.module.scala

import tools.jackson.databind.JacksonModule.SetupContext
import tools.jackson.module.scala.deser.EnumDeserializerModule
import tools.jackson.module.scala.ser.EnumSerializerModule
import tools.jackson.module.scala.util.Scala3EnumInfo

/**
 * The state Scala 3 enum support keeps, held per module instance rather than globally, so that a
 * `ScalaModule` built through [[ScalaModule.Builder]] has a cache and cache settings of its own.
 * Mixing both halves of the module together yields one shared instance.
 *
 * @since 3.3.0
 */
trait Scala3EnumSupportState {

  private[scala] val scala3EnumInfo: Scala3EnumInfo = new Scala3EnumInfo

  /**
   * Replaces the [[LookupCacheFactory]] used by this module instance for its cache of Scala 3 enum
   * case tables. The default factory uses [[tools.jackson.databind.util.SimpleLookupCache]].
   * <p>
   * Note that this clears the existing cache entries. The cache is only a memo of what can be
   * derived reflectively from an enum class, so clearing it affects performance but not behaviour.
   * </p>
   *
   * @param lookupCacheFactory new factory
   * @see [[setEnumInfoCacheSize]]
   * @since 3.3.0
   */
  def setLookupCacheFactory(lookupCacheFactory: LookupCacheFactory): Unit =
    scala3EnumInfo.setLookupCacheFactory(lookupCacheFactory)

  /**
   * Resize this module instance's cache of Scala 3 enum case tables. The default size is 1000.
   * <p>
   * The cache is bounded so that enum classes loaded by a short lived classloader are not retained
   * indefinitely. Entries are keyed by class, and an evicted entry is rebuilt on the next lookup.
   * </p>
   * <p>
   * Note that this clears the existing cache entries.
   * </p>
   *
   * @param size new size for the cache
   * @see [[setLookupCacheFactory]]
   * @since 3.3.0
   */
  def setEnumInfoCacheSize(size: Int): Unit = scala3EnumInfo.setCacheSize(size)

  /**
   * Empties this module instance's cache of Scala 3 enum case tables.
   *
   * @since 3.3.0
   */
  def clearEnumInfoCache(): Unit = scala3EnumInfo.clearCache()
}

trait EnumModule extends EnumSerializerModule with EnumDeserializerModule {
  override def getModuleName: String = "EnumModule"

  // built from this instance's state, not from the two singleton halves
  override def getInitializers(config: ScalaModule.Config): Seq[SetupContext => Unit] =
    serializerInitializers(config) ++ deserializerInitializers(config)
}

object EnumModule extends EnumModule
