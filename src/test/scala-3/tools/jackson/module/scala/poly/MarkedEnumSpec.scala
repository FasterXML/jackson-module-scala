package tools.jackson.module.scala.poly

import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.scala.{DefaultScalaModule, SealedPolymorphismSupport}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

// a Scala 3 enum carrying the marker - the enum support owns it, from its exact case table
enum Status extends SealedPolymorphismSupport:
  case Active
  case Failed(code: Int)

case class Job(status: Status)

class MarkedEnumSpec extends AnyWordSpec with Matchers {
  private val mapper = JsonMapper.builder().addModule(DefaultScalaModule).build()

  "SealedPolymorphismModule" should {
    "let the enum support own a marked Scala 3 enum" in {
      // the enum rules apply throughout: a simple case is its plain name, a parameterized one an object
      mapper.writeValueAsString(Job(Status.Active)) shouldEqual """{"status":"Active"}"""
      mapper.writeValueAsString(Job(Status.Failed(1))) shouldEqual """{"status":{"@type":"Failed","code":1}}"""
      val json = mapper.writeValueAsString(Job(Status.Failed(1)))
      mapper.readValue(json, classOf[Job]) shouldEqual Job(Status.Failed(1))
      mapper.readValue(mapper.writeValueAsString(Job(Status.Active)), classOf[Job]).status should
        be theSameInstanceAs Status.Active
    }
  }
}
