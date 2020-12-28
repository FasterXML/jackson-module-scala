package com.fasterxml.jackson.module.scala.ser

import java.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.{BaseSpec, DefaultScalaModule}
import io.swagger.models.ModelImpl
import io.swagger.models.properties.{IntegerProperty, Property}

class SwaggerTest extends BaseSpec {
  //https://github.com/FasterXML/jackson-module-scala/issues/454
  "An ObjectMapper" should "serialize a Swagger Model" in {
    val mapper = new ObjectMapper
    mapper.registerModule(new DefaultScalaModule)
    val model = new ModelImpl
    val property = new IntegerProperty
    val map = new util.HashMap[String, Property]()
    map.put("foo", property)
    model.setProperties(map)
    val json = mapper.writeValueAsString(model)
    json should include (""""foo":""")
  }
}
