package tools.jackson.module.scala.`enum`

import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.scala.{DefaultScalaModule, EnumModule, ScalaModule}
import tools.jackson.module.scala.deser.EnumDeserializerModule
import tools.jackson.module.scala.ser.EnumSerializerModule
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * The cache of enum case tables belongs to a module instance, so a mapper built from its own
 * ScalaModule does not share it with every other mapper in the process.
 */
class EnumModuleStateSpec extends AnyWordSpec with Matchers {

  "EnumModule" should {
    "give each module instance state of its own" in {
      val first = new EnumModule {}
      val second = new EnumModule {}
      first.scala3EnumInfo should not be theSameInstanceAs(second.scala3EnumInfo)
      EnumModule.scala3EnumInfo should not be theSameInstanceAs(first.scala3EnumInfo)
    }
    "share one instance between the two halves of a module" in {
      val module = new EnumModule {}
      (module: EnumSerializerModule).scala3EnumInfo should be theSameInstanceAs
        (module: EnumDeserializerModule).scala3EnumInfo
    }
    "tie the state of a built ScalaModule to that build" in {
      val builder = ScalaModule.builder().addAllBuiltinModules()
      val mapper = JsonMapper.builder().addModule(builder.build()).build()
      val instance = Result(ResultEnum.Ok("my-result"))
      mapper.writeValueAsString(instance) shouldEqual """{"result":{"@type":"Ok","value":"my-result"}}"""
      mapper.readValue(mapper.writeValueAsString(instance), classOf[Result]) shouldEqual instance
    }
    "still recognise the module object when removing it from a builder" in {
      val builder = ScalaModule.builder().addAllBuiltinModules()
      builder.hasModule(EnumModule) shouldEqual true
      builder.removeModule(EnumModule)
      builder.hasModule(EnumModule) shouldEqual false
      // without the enum module the parameterized case is no longer tagged
      val mapper = JsonMapper.builder().addModule(builder.build()).build()
      mapper.writeValueAsString(Result(ResultEnum.Ok("my-result"))) should not include "@type"
    }
    "rebuild a case table after the instance cache is cleared" in {
      val mapper = JsonMapper.builder().addModule(DefaultScalaModule).build()
      val instance = Result(ResultEnum.Error(123))
      mapper.writeValueAsString(instance) shouldEqual """{"result":{"@type":"Error","code":123}}"""
      EnumModule.clearEnumInfoCache()
      val fresh = JsonMapper.builder().addModule(DefaultScalaModule).build()
      fresh.readValue(fresh.writeValueAsString(instance), classOf[Result]) shouldEqual instance
    }
    "leave one module instance's cache untouched when another is cleared" in {
      val mine = new EnumModule {}
      val mapper = JsonMapper.builder().addModule(ScalaModule.builder().addAllBuiltinModules().build()).build()
      val instance = Result(ResultEnum.Ok("my-result"))
      mapper.writeValueAsString(instance) shouldEqual """{"result":{"@type":"Ok","value":"my-result"}}"""
      mine.clearEnumInfoCache()
      EnumModule.clearEnumInfoCache()
      mapper.readValue(mapper.writeValueAsString(instance), classOf[Result]) shouldEqual instance
    }
  }
}
