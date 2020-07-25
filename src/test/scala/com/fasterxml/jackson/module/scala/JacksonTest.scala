package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.{Module, ObjectMapper}

abstract class JacksonTest extends BaseSpec {
  def module: Module

  def newBuilder: JsonMapper.Builder = {
    JsonMapper.builder().addModule(module)
  }

  def newMapper: ObjectMapper = {
    newBuilder.build()
  }
}
