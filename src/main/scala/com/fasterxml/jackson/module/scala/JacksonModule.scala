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

  protected def +=(ser: Serializers): this.type = { initializers += (_ addSerializers ser); this }
  protected def +=(deser: Deserializers): this.type = { initializers += (_ addDeserializers deser); this }
  protected def +=(typeMod: TypeModifier): this.type = { initializers += (_ addTypeModifier typeMod); this }

  protected def +=(beanSerMod: BeanSerializerModifier): this.type = {
    initializers += (_ addBeanSerializerModifier beanSerMod)
    this
  }

}