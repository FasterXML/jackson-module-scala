package tools.jackson.module.other

import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.scala.{DefaultScalaModule, ScalaTypeInfo}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

// declared where a user would declare it, rather than inside the module's own package
case class Outside(amount: Option[Long]) derives ScalaTypeInfo

/**
 * Deriving works from any package, while what it captures stays inside the module - a user cannot
 * reach `erasedTypeArguments`, so what it carries can still be changed.
 */
class OutsidePackageSpec extends AnyWordSpec with Matchers {
  "ScalaTypeInfo" should {
    "be derivable from a package of the user's own" in {
      val mapper = JsonMapper.builder().addModule(DefaultScalaModule).build()
      val read = mapper.readValue("""{"amount":2}""", classOf[Outside])
      read.amount.map(_ + 1L) shouldEqual Some(3L)
    }
  }
}
