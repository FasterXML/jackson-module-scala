package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.databind.{Module, ObjectMapper}

abstract class JacksonTest extends BaseSpec {

  def module: Module

  def mapper = {
    val result = new ObjectMapper
    result.registerModule(module)
    result
  }

}