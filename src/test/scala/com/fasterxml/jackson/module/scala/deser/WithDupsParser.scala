package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.{JsonParser, StreamReadCapability}
import com.fasterxml.jackson.core.util.{JacksonFeatureSet, JsonParserDelegate}

class WithDupsParser(p: JsonParser) extends JsonParserDelegate(p) {
  override def getReadCapabilities: JacksonFeatureSet[StreamReadCapability] = {
    super.getReadCapabilities.`with`(StreamReadCapability.DUPLICATE_PROPERTIES)
  }
}
