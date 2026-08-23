package tools.jackson.module.scala.`enum`

import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.scala.{DefaultLookupCacheFactory, DefaultScalaModule, EnumModule}
import tools.jackson.module.scala.`enum`.adt.{Color, ColorSet}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * The case tables that back Scala 3 enum support are cached, and that cache is bounded. An evicted
 * entry has to be rebuilt on demand, so eviction must not be observable in what gets written or read.
 */
class EnumInfoCacheSpec extends AnyWordSpec with Matchers {

  private val DefaultCacheSize = 1000

  // a mapper caches the serializers and deserializers it has resolved, and those hold onto the case
  // table they were built with - a new mapper is needed to force the table to be looked up again
  private def newMapper() = JsonMapper.builder().addModule(DefaultScalaModule).build()

  "EnumModule" should {
    "rebuild a case table that has been evicted" in {
      val instance = Result(ResultEnum.Ok("my-result"))
      val json = newMapper().writeValueAsString(instance)
      EnumModule.clearEnumInfoCache()
      newMapper().readValue(json, classOf[Result]) shouldEqual instance
    }
    "rebuild a case table for a simple case that has been evicted" in {
      val json = newMapper().writeValueAsString(Result(ResultEnum.Pending))
      EnumModule.clearEnumInfoCache()
      // simple cases are read back from the enum companion, so the singleton is preserved
      newMapper().readValue(json, classOf[Result]).result should be theSameInstanceAs ResultEnum.Pending
    }
    "handle a cache too small to hold every enum" in {
      try {
        EnumModule.setEnumInfoCacheSize(1)
        val result = Result(ResultEnum.Error(123))
        val colors = ColorSet(Set(Color.Red, Color.Mix(0x4488FF)))
        // each enum touched here evicts the one before it
        newMapper().readValue(newMapper().writeValueAsString(result), classOf[Result]) shouldEqual result
        newMapper().readValue(newMapper().writeValueAsString(colors), classOf[ColorSet]) shouldEqual colors
        newMapper().readValue(newMapper().writeValueAsString(result), classOf[Result]) shouldEqual result
      } finally {
        EnumModule.setEnumInfoCacheSize(DefaultCacheSize)
      }
    }
    "keep working after the lookup cache factory is replaced" in {
      val instance = Result(ResultEnum.Ok("my-result"))
      val json = newMapper().writeValueAsString(instance)
      EnumModule.setLookupCacheFactory(DefaultLookupCacheFactory)
      newMapper().readValue(json, classOf[Result]) shouldEqual instance
    }
  }
}
