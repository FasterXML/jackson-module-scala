package com.fasterxml.jackson.module.scala

import org.codehaus.jackson.map.{Module, ObjectMapper}

trait JacksonTest {

  val module: Module

  val mapper = {
    val result = new ObjectMapper
    result.registerModule(module)
    result
  }

}