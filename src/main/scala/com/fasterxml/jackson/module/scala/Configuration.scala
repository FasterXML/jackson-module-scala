package com.fasterxml.jackson.module.scala

import com.typesafe.config.{Config, ConfigFactory}

object Configuration {
  private var moduleConfig = ConfigFactory.load()

  def getModuleConfig(): Config = moduleConfig

  def setModuleConfig(moduleConfig: Config): Unit = {
    this.moduleConfig = moduleConfig
  }
}
