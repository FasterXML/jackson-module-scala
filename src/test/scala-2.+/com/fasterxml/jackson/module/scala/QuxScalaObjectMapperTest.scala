package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.databind.json.JsonMapper

class QuxScalaObjectMapperTest extends BaseSpec {
  "An ObjectMapper with the ScalaObjectMapper mixin" should "deserialize Qux" in {
    val objectMapper = JsonMapper
        .builder().addModule(DefaultScalaModule).build() :: ScalaObjectMapper
    val qux = objectMapper.readValue[Qux]("""{"qux": {"num": "3"}}""")
    qux.qux.num shouldEqual 3
  }
}
