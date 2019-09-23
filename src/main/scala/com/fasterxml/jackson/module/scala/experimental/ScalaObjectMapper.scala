package com.fasterxml.jackson.module.scala.experimental

import com.fasterxml.jackson.databind.ObjectMapper

/**
 * @deprecated use {@link com.fasterxml.jackson.module.scala.ScalaObjectMapper}
 */
@Deprecated
trait ScalaObjectMapper extends com.fasterxml.jackson.module.scala.ScalaObjectMapper {
  self: ObjectMapper =>
}
