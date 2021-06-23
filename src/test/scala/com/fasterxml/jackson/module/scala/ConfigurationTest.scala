package com.fasterxml.jackson.module.scala

import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterEach

class ConfigurationTest extends BaseSpec with BeforeAndAfterEach {

  val configName = "jackson.module.scala.deserializer.apply.default.values"

  override def afterEach(): Unit = {
    super.afterEach()
    Configuration.resetModuleConfig()
  }

  "Configuration" should "support getting config" in {
    Configuration.getModuleConfig().getBoolean(configName) shouldBe true
  }
  it should "support overriding config" in {
    Configuration.setModuleConfig(ConfigFactory.parseString(s"$configName=false"))
    Configuration.getModuleConfig().getBoolean(configName) shouldBe false
  }
  it should "support fallback to default config" in {
    Configuration.setModuleConfig(ConfigFactory.parseString("dummy.config=false"))
    Configuration.getModuleConfig().getBoolean(configName) shouldBe true
  }
}
