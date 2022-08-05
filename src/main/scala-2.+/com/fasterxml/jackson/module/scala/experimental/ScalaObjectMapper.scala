package com.fasterxml.jackson.module.scala.experimental

import com.fasterxml.jackson.databind.ObjectMapper

/**
 * @deprecated use [[com.fasterxml.jackson.module.scala.ScalaObjectMapper]]
 */
@deprecated("use com.fasterxml.jackson.module.scala.ScalaObjectMapper", "2.12.1")
trait ScalaObjectMapper extends com.fasterxml.jackson.module.scala.ScalaObjectMapper {
  self: ObjectMapper =>
}
