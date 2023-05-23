package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.databind.{BeanDescription, DeserializationConfig, JavaType, JsonDeserializer}
import com.fasterxml.jackson.databind.deser.Deserializers
import com.fasterxml.jackson.module.scala.deser.{ImmutableBitSetDeserializer, MutableBitSetDeserializer}

import scala.collection.{BitSet, immutable, mutable}

/**
 * Adds support for deserializing Scala [[scala.collection.BitSet]]s. Scala Bitsets can already be
 * serialized using [[IteratorModule]] or [[DefaultScalaModule]].
 * <p>
 * <b>Do not enable this module unless you are sure that no input is accepted from untrusted sources.</b>
 * </p>
 * Scala BitSets use memory based on the highest int value stored. So a BitSet with just one big int will use a lot
 * more memory than a Scala BitSet with many small ints stored in it.
 *
 * @since 2.14.0
 */
object BitSetDeserializerModule extends JacksonModule {
  override def getModuleName: String = "BitSetDeserializerModule"
  this += (_ addDeserializers new Deserializers.Base {

    private val IMMUTABLE_BITSET_CLASS: Class[_] = classOf[immutable.BitSet]
    private val MUTABLE_BITSET_CLASS: Class[_] = classOf[mutable.BitSet]

    override def findBeanDeserializer(javaType: JavaType, config: DeserializationConfig, beanDesc: BeanDescription): JsonDeserializer[BitSet] = {
      val rawClass = javaType.getRawClass
      if (IMMUTABLE_BITSET_CLASS.isAssignableFrom(rawClass)) {
        ImmutableBitSetDeserializer.asInstanceOf[JsonDeserializer[BitSet]]
      } else if (MUTABLE_BITSET_CLASS.isAssignableFrom(rawClass)) {
        MutableBitSetDeserializer.asInstanceOf[JsonDeserializer[BitSet]]
      } else {
        None.orNull
      }
    }
  })
}
