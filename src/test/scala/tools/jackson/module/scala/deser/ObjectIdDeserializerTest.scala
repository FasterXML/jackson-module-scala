package tools.jackson.module.scala.deser

import tools.jackson.module.scala.DefaultScalaModule
import tools.jackson.module.scala.ser.ObjectIdSerializerTest

class ObjectIdDeserializerTest extends DeserializerTest {
  import ObjectIdSerializerTest._
  lazy val module = DefaultScalaModule

  "An ObjectMapper" should "deserialize with ids when @JsonIdentityInfo is used" in {
    val f1 = Foo(1)
    val f2 = Foo(2)
    val data = ManyFooPairs(Seq(FooPair(f1, f1), FooPair(f1, f2)))
    val json = serialize(data)
    val result = deserialize(json, classOf[ManyFooPairs])
    result shouldEqual data
  }
}
