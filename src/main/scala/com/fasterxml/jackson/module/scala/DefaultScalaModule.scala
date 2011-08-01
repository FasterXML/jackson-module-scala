package com.fasterxml.jackson.module.scala

import org.codehaus.jackson.map.Module.SetupContext

class DefaultScalaModule
  extends JacksonModule
     with SeqModule
{
  override def getModuleName = "DefaultScalaModule"

  // Unfortunate repetition to allow Java code to call this
  override def setupModule(context: SetupContext) { super.setupModule(context) }
}