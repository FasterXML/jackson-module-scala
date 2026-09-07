package tools.jackson.module.scala

class DefaultScalaModuleTest extends BaseSpec {

  "DefaultScalaModule" should "have a sensible version" in {
    val version = DefaultScalaModule.version
    version.getMajorVersion should be >= 2
    version.getArtifactId should be ("jackson-module-scala")
    version.getGroupId should be ("tools.jackson.module")
  }

  it should "read the build properties the version is built from" in {
    val props = JacksonModule.buildProps
    props.get("groupId") should be (Some("tools.jackson.module"))
    props.get("artifactId") should be (Some("jackson-module-scala"))
    props.get("version") should not be empty
  }
}
