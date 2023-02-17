package tools.jackson.module.scala

import tools.jackson.databind.util.{LookupCache, SimpleLookupCache}

/**
 * Factory for creating [[com.fasterxml.jackson.databind.util.LookupCache]] instances
 *
 * @since 2.14.3
 */
trait LookupCacheFactory {
  def createLookupCache[K, V](initialEntries: Int, maxEntries: Int): LookupCache[K, V]
}

object DefaultLookupCacheFactory extends LookupCacheFactory {
  override def createLookupCache[K, V](initialEntries: Int, maxEntries: Int): LookupCache[K, V] = {
    new SimpleLookupCache[K, V](initialEntries, maxEntries)
  }
}
