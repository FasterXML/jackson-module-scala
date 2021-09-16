package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.databind.json.JsonMapper

class QuxClassTagExtensionsTest extends BaseSpec {
  "An ObjectMapper with the ClassTagExtensions mixin" should "deserialize Qux" in {
    val objectMapper = JsonMapper
      .builder().addModule(DefaultScalaModule).build() :: ClassTagExtensions
    val qux = objectMapper.readValue[Qux]("""{"qux": {"num": "3"}}""")
    qux.qux.num shouldEqual 3
  }
}
