package com.fasterxml.jackson.module.scala

import org.codehaus.jackson.Version
import org.codehaus.jackson.map.Module.SetupContext
import org.codehaus.jackson.map.{Deserializers, Serializers, Module}
import org.codehaus.jackson.map.`type`.TypeModifier
import org.codehaus.jackson.map.ser.BeanSerializerModifier

trait JacksonModule extends Module {

  private val initializers = Seq.newBuilder[SetupContext => Unit]

  def getModuleName = "JacksonModule"

  // TODO: Keep in sync with POM
  val version = new Version(1,9,0,"SNAPSHOT")

  def setupModule(context: SetupContext) { initializers result() foreach (_ apply context) }

  protected def +=(init: SetupContext => Unit): this.type = { initializers += init; this }
  protected def +=(ser: Serializers): this.type = this += (_ addSerializers ser)
  protected def +=(deser: Deserializers): this.type = this += (_ addDeserializers deser)
  protected def +=(typeMod: TypeModifier): this.type = this += (_ addTypeModifier typeMod)
  protected def +=(beanSerMod: BeanSerializerModifier): this.type = this += (_ addBeanSerializerModifier beanSerMod)

}