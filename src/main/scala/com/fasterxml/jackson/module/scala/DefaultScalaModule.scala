package com.fasterxml.jackson.module.scala

class DefaultScalaModule
  extends JacksonModule
     with EnumerationModule
     with OptionModule
     with SeqModule
     with IterableModule
     with TupleModule
{
  override def getModuleName = "DefaultScalaModule"
}