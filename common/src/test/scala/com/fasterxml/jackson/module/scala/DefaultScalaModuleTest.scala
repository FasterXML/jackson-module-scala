package com.fasterxml.jackson.module.scala

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class DefaultScalaModuleTest extends BaseSpec {

  "DefaultScalaModule" should "have a sensible version" in {
    val version = DefaultScalaModule.version
    version.getMajorVersion should be >= 2
    version.getArtifactId should be ("jackson-module-scala")
    version.getGroupId should be ("com.fasterxml.jackson.module")
  }

}
