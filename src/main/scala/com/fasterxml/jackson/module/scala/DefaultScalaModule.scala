package com.fasterxml.jackson.module.scala

import org.codehaus.jackson.map.Module.SetupContext

class DefaultScalaModule
  extends JacksonModule
     with OptionModule
     with SeqModule
     with TupleModule
{
  override def getModuleName = "DefaultScalaModule"

  // Unfortunate repetition to allow Java code to call this
  override def setupModule(context: SetupContext) { super.setupModule(context) }
}