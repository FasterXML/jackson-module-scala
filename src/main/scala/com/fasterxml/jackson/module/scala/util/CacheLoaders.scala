package com.fasterxml.jackson.module.scala.util

import com.google.common.cache.CacheLoader

trait CacheLoaders {
  val DEFAULT_CACHE_SIZE = 50

  implicit def mkCacheLoader[K,V](f: (K) => V) = new CacheLoader[K,V] {
    def load(key: K) = f(key)
  }
}
