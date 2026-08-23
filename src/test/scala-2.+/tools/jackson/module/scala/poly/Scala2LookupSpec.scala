package tools.jackson.module.scala.poly

import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.scala.DefaultScalaModule
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * Scala 2 keeps its symbol table in the class file, so scala-reflect can enumerate a hierarchy and
 * confirm it is `sealed`. Both let a problem be reported for the hierarchy as a whole, rather than
 * one implementation at a time as on Scala 3.
 */
class Scala2LookupSpec extends AnyWordSpec with Matchers {
  private val mapper = JsonMapper.builder().addModule(DefaultScalaModule).build()

  private def rootCause(error: Throwable): Throwable =
    Option(error.getCause).filter(_ ne error).map(rootCause).getOrElse(error)

  private def failureFor(value: Any): String =
    String.valueOf(rootCause(intercept[Exception](mapper.writeValueAsString(value))).getMessage)

  "SealedPolymorphismModule on Scala 2" should {
    "reject every implementation of a base that is not sealed" in {
      // including this one, which happens to sit where resolution would have found it
      val message = failureFor(UnsealedHolder(Nearby(1)))
      message should include("Unsealed")
      message should include("is not sealed")
    }
    "name the hierarchy rather than the implementation when the base is not sealed" in {
      failureFor(UnsealedHolder(other.Faraway(1))) should include("is not sealed")
    }
    "reject both implementations when a derived name is claimed twice" in {
      failureFor(ShadowHolder(Shadow.Entry(1))) should include("already belongs to")
      failureFor(ShadowHolder(Entry("x"))) should include("already belongs to")
    }
  }
}
