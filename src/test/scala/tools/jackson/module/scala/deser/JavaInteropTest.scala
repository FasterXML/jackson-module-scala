package tools.jackson.module.scala.deser

import tools.jackson.module.scala.BaseSpec

class JavaInteropTest extends BaseSpec {
  "Scala module" should "interoperate with Java classes" in {
    val v = Util.mapper.readValue(Util.jsonString, classOf[B])
    v shouldEqual new B("asdf", new A1("qwer"))
  }
}
