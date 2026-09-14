package tools.jackson.module.scala.deser

import tools.jackson.core.`type`.TypeReference
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.module.SimpleModule
import tools.jackson.module.scala.{BaseSpec, DefaultScalaModule}

import scala.collection.immutable.ListMap

// deserialized, so declared where Jackson can construct it - an inner case class cannot be
case class ListMapMappedHolder(m: Map[String, Int], any: Any)

/**
 * Shows how a mapper is set up so that JSON objects are read into a `ListMap`, which keeps the
 * order of the keys, rather than the `HashMap` a `Map` is read into by default - both where the
 * target is declared as a `Map` and where it is `Any`, however deep the nesting.
 *
 * Nothing in the Scala module is configured for this: Jackson's own abstract type mapping is enough.
 * The untyped deserializer asks the mapper what `scala.collection.Map` maps to, and a field typed as
 * `Map` is `scala.collection.immutable.Map`, so both are mapped.
 *
 * @see [[https://github.com/FasterXML/jackson-module-scala/issues/783]]
 */
class ListMapAbstractTypeMappingTest extends BaseSpec {

  private val mapper = {
    val listMapModule = new SimpleModule("list-maps")
      .addAbstractTypeMapping(classOf[collection.Map[_, _]], classOf[ListMap[_, _]])
      .addAbstractTypeMapping(classOf[Map[_, _]], classOf[ListMap[_, _]])
    JsonMapper.builder()
      .addModule(DefaultScalaModule)
      .addModule(listMapModule)
      .build()
  }

  // enough keys that a hash-based map would not happen to keep them in order
  private val keys = ('a' to 'p').map(_.toString)
  private val flatJson = keys.zipWithIndex.map { case (k, i) => s""""$k":$i""" }.mkString("{", ",", "}")
  private val nestedJson = keys.zipWithIndex.map { case (k, i) => s""""$k":{"nested":$flatJson,"index":$i}""" }.mkString("{", ",", "}")

  behavior of "A mapper with an abstract type mapping from Map to ListMap"

  it should "read a JSON object declared as a Map into a ListMap in key order" in {
    val result = mapper.readValue(flatJson, new TypeReference[Map[String, Int]] {})
    result shouldBe a[ListMap[_, _]]
    result.keys.toSeq shouldBe keys
  }

  it should "read a JSON object declared as Any into a ListMap in key order" in {
    val result = mapper.readValue(flatJson, classOf[Any])
    result shouldBe a[ListMap[_, _]]
    result.asInstanceOf[ListMap[String, Any]].keys.toSeq shouldBe keys
  }

  it should "read nested JSON objects declared as Map[String, Any] into ListMaps at every level" in {
    val result = mapper.readValue(nestedJson, new TypeReference[Map[String, Any]] {})
    result shouldBe a[ListMap[_, _]]
    result.keys.toSeq shouldBe keys
    result.values.foreach { value =>
      value shouldBe a[ListMap[_, _]]
      val inner = value.asInstanceOf[ListMap[String, Any]]
      inner.keys.toSeq shouldBe Seq("nested", "index")
      inner("nested") shouldBe a[ListMap[_, _]]
      inner("nested").asInstanceOf[ListMap[String, Any]].keys.toSeq shouldBe keys
    }
  }

  it should "read the Map and Any fields of a case class into ListMaps" in {
    val result = mapper.readValue(s"""{"m":$flatJson,"any":$nestedJson}""", classOf[ListMapMappedHolder])
    result.m shouldBe a[ListMap[_, _]]
    result.m.keys.toSeq shouldBe keys
    result.any shouldBe a[ListMap[_, _]]
    result.any.asInstanceOf[ListMap[String, Any]].keys.toSeq shouldBe keys
  }

  it should "still read a JSON object into a HashMap without the mapping" in {
    val plain = JsonMapper.builder().addModule(DefaultScalaModule).build()
    plain.readValue(flatJson, classOf[Any]) should not be a[ListMap[_, _]]
    plain.readValue(flatJson, new TypeReference[Map[String, Int]] {}) should not be a[ListMap[_, _]]
  }
}
