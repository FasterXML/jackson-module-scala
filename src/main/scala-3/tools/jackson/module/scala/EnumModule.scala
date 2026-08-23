package tools.jackson.module.scala

import tools.jackson.databind.JacksonModule.SetupContext
import tools.jackson.module.scala.deser.EnumDeserializerModule
import tools.jackson.module.scala.ser.EnumSerializerModule
import tools.jackson.module.scala.util.Scala3EnumInfo

trait EnumModule extends EnumSerializerModule with EnumDeserializerModule {
  override def getModuleName: String = "EnumModule"

  override def getInitializers(config: ScalaModule.Config): Seq[SetupContext => Unit] = {
    EnumSerializerModule.getInitializers(config) ++
      EnumDeserializerModule.getInitializers(config)
  }
}

object EnumModule extends EnumModule {

  /**
   * Replaces the [[LookupCacheFactory]] used for the cache of Scala 3 enum case tables. The default
   * factory uses [[tools.jackson.databind.util.SimpleLookupCache]].
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
    Scala3EnumInfo.setLookupCacheFactory(lookupCacheFactory)

  /**
   * Resize the cache of Scala 3 enum case tables. The default size is 1000.
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
  def setEnumInfoCacheSize(size: Int): Unit = Scala3EnumInfo.setCacheSize(size)

  /**
   * Empties the cache of Scala 3 enum case tables.
   *
   * @since 3.3.0
   */
  def clearEnumInfoCache(): Unit = Scala3EnumInfo.clearCache()
}
