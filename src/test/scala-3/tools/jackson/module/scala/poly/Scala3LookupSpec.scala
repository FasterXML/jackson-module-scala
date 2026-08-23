package tools.jackson.module.scala.poly

import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.scala.DefaultScalaModule
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * Scala 3 records nothing about `sealed` on the JVM, so a hierarchy can only be judged one
 * implementation at a time, by whether that implementation can be found again from its name. An
 * implementation that happens to sit where resolution looks is accepted even if the base is not
 * sealed - Scala 2 rejects the same hierarchy outright, so it is the stricter of the two.
 */
class Scala3LookupSpec extends AnyWordSpec with Matchers {
  private val mapper = JsonMapper.builder().addModule(DefaultScalaModule).build()

  private def roundTrip[T](value: T, clazz: Class[T]): T = mapper.readValue(mapper.writeValueAsString(value), clazz)

  "SealedPolymorphismModule on Scala 3" should {
    "accept an implementation declared alongside a base that is not sealed" in {
      roundTrip(UnsealedHolder(Nearby(1)), classOf[UnsealedHolder]) shouldEqual UnsealedHolder(Nearby(1))
    }
    "keep the implementation declared closer to the root when a name is claimed twice" in {
      roundTrip(ShadowHolder(Shadow.Entry(1)), classOf[ShadowHolder]) shouldEqual ShadowHolder(Shadow.Entry(1))
    }
  }
}
