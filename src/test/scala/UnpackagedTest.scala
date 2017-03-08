import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.ser.SerializerTest
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner

case class UnpackagedCaseClass(intValue: Int, stringValue: String)

@RunWith(classOf[JUnitRunner])
class UnpackagedTest extends SerializerTest {

  def module = DefaultScalaModule

  behavior of "DefaultScalaModule"

  it should "serialize a case class not in a package" in {
    val result = serialize(UnpackagedCaseClass(1, "foo"))
    result should be ("""{"intValue":1,"stringValue":"foo"}""")
  }

}
