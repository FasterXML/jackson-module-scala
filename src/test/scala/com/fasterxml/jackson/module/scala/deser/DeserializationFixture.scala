package com.fasterxml.jackson.module.scala.deser

import org.scalatest.{Succeeded, Outcome, Matchers, fixture}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.{BaseFixture, DefaultScalaModule}
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper

class DeserializationFixture extends BaseFixture
