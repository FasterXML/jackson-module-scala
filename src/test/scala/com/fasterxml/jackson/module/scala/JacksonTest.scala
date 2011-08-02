package com.fasterxml.jackson.module.scala

import org.codehaus.jackson.map.ObjectMapper

trait JacksonTest {

  def module: JacksonModule

  val mapper = {
    val result = new ObjectMapper
    result.registerModule(module)
    result
  }

}