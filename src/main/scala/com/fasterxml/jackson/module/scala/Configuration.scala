package com.fasterxml.jackson.module.scala

import com.typesafe.config.{Config, ConfigFactory}

object Configuration {
  private val defaultConfig = ConfigFactory.load()
  private var moduleConfig = defaultConfig

  def getModuleConfig(): Config = moduleConfig

  def setModuleConfig(moduleConfig: Config): Unit = {
    this.moduleConfig = moduleConfig.withFallback(defaultConfig)
  }

  def resetModuleConfig(): Unit = {
    this.moduleConfig = defaultConfig
  }
}
