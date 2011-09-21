package com.fasterxml.jackson.module.scala

class DefaultScalaModule
  extends JacksonModule
     with EnumerationModule
     with OptionModule
     with SeqModule
     with TupleModule
{
  override def getModuleName = "DefaultScalaModule"
}