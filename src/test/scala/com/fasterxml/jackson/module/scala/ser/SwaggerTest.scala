package com.fasterxml.jackson.module.scala.ser

import java.util

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.{BaseSpec, DefaultScalaModule}
import io.swagger.models.ModelImpl
import io.swagger.models.properties.{IntegerProperty, Property}

class SwaggerTest extends BaseSpec {
  //https://github.com/FasterXML/jackson-module-scala/issues/454
  "An ObjectMapper" should "serialize a Swagger Model" in {
    val builder = JsonMapper.builder().addModule(new DefaultScalaModule)
    val mapper = builder.build()
    val model = new ModelImpl
    val property = new IntegerProperty
    val map = new util.HashMap[String, Property]()
    map.put("foo", property)
    model.setProperties(map)
    val json = mapper.writeValueAsString(model)
    json should include (""""foo":""")
  }
}
