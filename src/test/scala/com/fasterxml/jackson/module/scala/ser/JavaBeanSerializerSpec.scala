package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.{DefaultScalaModule, JacksonModule, JacksonTest}

class JavaBeanSerializerSpec extends JacksonTest {

  val module: JacksonModule = DefaultScalaModule

  it should "serialize Java Bean as expected without scala module" in {
    val mapper = JsonMapper.builder().build()
    val json = mapper.writeValueAsString(new JavaBean())
    json should include(""""booleanProperty":true""")
    json should include(""""anotherBooleanProperty":false""")
    json should include(""""string":"value"""")
  }
  it should "serialize Java Bean as expected with scala module" in {
    val mapper = JsonMapper.builder().addModule(DefaultScalaModule).build()
    val json = mapper.writeValueAsString(new JavaBean())
    json should include(""""booleanProperty":true""")
    json should include(""""anotherBooleanProperty":false""")
    json should include(""""string":"value"""")
  }
}
