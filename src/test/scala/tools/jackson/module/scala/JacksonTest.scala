package tools.jackson.module.scala

import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.{JacksonModule => DatabindModule}

abstract class JacksonTest extends BaseSpec {
  def module: DatabindModule

  def newBuilder: JsonMapper.Builder = {
    JsonMapper.builder().addModule(module)
  }

  def newMapper: JsonMapper = newBuilder.build()
}
