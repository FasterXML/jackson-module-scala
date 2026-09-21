package tools.jackson.module.scala.ser

import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.scala.ser.TupleSerializerTest.OptionalTupleHolder
import tools.jackson.module.scala.{DefaultScalaModule, JacksonModule}

object TupleSerializerTest {
  case class OptionalTupleHolder(tuple: (Option[Int], Option[String]))
}

class TupleSerializerTest extends SerializerTest {
  lazy val module = new JacksonModule with TupleSerializerModule

  "An ObjectMapper" should "serialize a Tuple2" in {
    val result = serialize("A" -> 1)
    result should be ("""["A",1]""")
  }

  it should "serialize a Tuple3" in {
    val result = serialize((3.0, "A", 1))
    result should be ("""[3.0,"A",1]""")
  }

  it should "serialize an OptionalTupleHolder" in {
    val mapper = JsonMapper.builder().addModule(DefaultScalaModule).build()
    val result = serialize(OptionalTupleHolder(Some(1), Some("one")), mapper)
    result should be("""{"tuple":[1,"one"]}""")
  }

  it should "serialize an OptionalTupleHolder with nulls" in {
    val mapper = JsonMapper.builder().addModule(DefaultScalaModule).build()
    val result = serialize(OptionalTupleHolder(None, None), mapper)
    result should be("""{"tuple":[null,null]}""")
  }

  it should "serialize a Tuple1" in {
    serialize(Tuple1("a")) should be ("""["a"]""")
  }

  it should "serialize a null element as null" in {
    serialize((1, null): (Int, String)) should be ("[1,null]")
  }

  it should "serialize nested tuples" in {
    val mapper = JsonMapper.builder().addModule(DefaultScalaModule).build()
    serialize(((1, "z"), List(("a", 1), ("b", 2))), mapper) should be ("""[[1,"z"],[["a",1],["b",2]]]""")
  }

  // Scala 2 instantiates (Int, Int) as the Tuple2$mcII$sp subclass and so on; the serializer
  // must be resolved for those runtime classes as it is for the plain Tuple2
  it should "serialize the specialized Tuple2 kinds" in {
    serialize((1, 2)) should be ("[1,2]")
    serialize((1L, 2.5)) should be ("[1,2.5]")
    serialize((true, '中')) should be ("""[true,"中"]""")
  }
}
