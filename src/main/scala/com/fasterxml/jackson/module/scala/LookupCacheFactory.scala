package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.databind.util.{LRUMap, LookupCache}

/**
 * Factory for creating {@link LookupCache} instances
 */
trait LookupCacheFactory {
  def createLookupCache[K, V](initialEntries: Int, maxEntries: Int): LookupCache[K, V]
}

object DefaultLookupCacheFactory extends LookupCacheFactory {
  override def createLookupCache[K, V](initialEntries: Int, maxEntries: Int): LookupCache[K, V] = {
    new LRUMap[K, V](initialEntries, maxEntries)
  }
}
