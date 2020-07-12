package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.module.scala.BaseSpec
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class JavaInteropTest extends BaseSpec {
  "Scala module" should "interoperate with Java classes" in {
    val v = Util.mapper.readValue(Util.jsonString, classOf[B])
    v shouldEqual new B("asdf", new A1("qwer"))
  }
}
