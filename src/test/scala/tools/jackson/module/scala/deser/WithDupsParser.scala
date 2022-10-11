package tools.jackson.module.scala.deser

import tools.jackson.core.{JsonParser, StreamReadCapability}
import tools.jackson.core.util.{JacksonFeatureSet, JsonParserDelegate}

class WithDupsParser(p: JsonParser) extends JsonParserDelegate(p) {
  override def streamReadCapabilities(): JacksonFeatureSet[StreamReadCapability] = {
    super.streamReadCapabilities().`with`(StreamReadCapability.DUPLICATE_PROPERTIES)
  }
}
