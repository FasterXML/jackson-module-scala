package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.databind.json.JsonMapper

class QuxClassTagExtensionsTest extends BaseSpec {
  "An ObjectMapper with the ClassTagExtensions mixin" should "deserialize Qux" in {
    val javaVersion = System.getProperty("java.specification.version")
    if (javaVersion == "1.8") {
      //skip - this test fails on Java 1.8 - see https://github.com/FasterXML/jackson-module-scala/issues/542
    } else {
      val objectMapper = JsonMapper
        .builder().addModule(DefaultScalaModule).build() :: ClassTagExtensions
      val qux = objectMapper.readValue[Qux]("""{"qux": {"num": "3"}}""")
      qux.qux.num shouldEqual 3
    }
  }
}
