package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.StreamReadCapability
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.`type`.MapLikeType

private[deser] object MapDeserializerUtil {
  private val objClass = classOf[Object]

  def squashDuplicateKeys(ctxt: DeserializationContext, mapType: MapLikeType): Boolean = {
    ctxt.isEnabled(StreamReadCapability.DUPLICATE_PROPERTIES) &&
      objClass == mapType.getContentType.getRawClass
  }
}
