package tools.jackson.module.scala.poly

import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.scala.{BaseSpec, DefaultScalaModule}

/**
 * A derived `@type` name for a hierarchy that involves an object nested inside another object. Such
 * an object is reported as an enclosing class by its module class, whose name ends in `$`, where a
 * top level object is reported by the class carrying its static forwarders, whose name does not -
 * so the two have to be measured differently to find where a name is nested.
 */
class NestedObjectNameSpec extends BaseSpec {

  private val mapper: JsonMapper = JsonMapper.builder().addModule(DefaultScalaModule).build()

  "An implementation declared beside its base in a doubly nested object" should "keep its own name" in {
    mapper.writeValueAsString(DeepHolder(Deep.Group.Leaf(1))) shouldEqual """{"base":{"@type":"Leaf","x":1}}"""
  }

  it should "keep its own name as a case object" in {
    mapper.writeValueAsString(DeepHolder(Deep.Group.Lone)) shouldEqual """{"base":{"@type":"Lone"}}"""
  }

  it should "round-trip" in {
    Seq[Deep.Group.Base](Deep.Group.Leaf(1), Deep.Group.Lone).foreach { value =>
      val json = mapper.writeValueAsString(DeepHolder(value))
      withClue(json) { mapper.readValue(json, classOf[DeepHolder]) shouldEqual DeepHolder(value) }
    }
  }

  "An implementation declared in an object nested below the base" should "keep that object in its name" in {
    mapper.writeValueAsString(SplitHolder(Deep.Held.Leaf(2))) shouldEqual """{"split":{"@type":"Held.Leaf","x":2}}"""
  }

  it should "round-trip" in {
    val json = mapper.writeValueAsString(SplitHolder(Deep.Held.Leaf(2)))
    mapper.readValue(json, classOf[SplitHolder]) shouldEqual SplitHolder(Deep.Held.Leaf(2))
  }
}
