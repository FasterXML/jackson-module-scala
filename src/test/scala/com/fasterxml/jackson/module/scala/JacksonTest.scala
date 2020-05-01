package __foursquare_shaded__.com.fasterxml.jackson.module.scala

import __foursquare_shaded__.com.fasterxml.jackson.databind.{Module, ObjectMapper}

abstract class JacksonTest extends BaseSpec {
  def module: Module

  def newMapper: ObjectMapper = {
    val result = new ObjectMapper
    result.registerModule(module)
    result
  }
}
