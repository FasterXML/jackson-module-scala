package com.fasterxml.jackson.module.scala

import org.codehaus.jackson.Version
import org.codehaus.jackson.map.Module.SetupContext
import org.codehaus.jackson.map.{Deserializers, Serializers, Module}
import org.codehaus.jackson.map.`type`.TypeModifier
import org.codehaus.jackson.map.ser.BeanSerializerModifier
import java.util.Properties

object JacksonModule {
  private val VersionRegex = """(\d+)\.(\d+)(?:\.(\d+)(?:\-(.*))?)?""".r
  private val cls = classOf[JacksonModule]
  private val buildPropsFilename = cls.getPackage.getName.replace('.','/') + "/build.properties"
  lazy val buildProps: Properties = {
    val props = new Properties
    val stream = cls.getClassLoader.getResourceAsStream(buildPropsFilename)
    if (stream ne null) props.load(stream)

    props
  }
  lazy val version: Version = {
    buildProps.getProperty("version") match {
      case VersionRegex(major,minor,patchOpt,snapOpt) => {
        val patch = Option(patchOpt) map (_.toInt) getOrElse 0
        new Version(major.toInt,minor.toInt,patch,snapOpt)
      }
      case _ => Version.unknownVersion()
    }
  }
}

trait JacksonModule extends Module {

  private val initializers = Seq.newBuilder[SetupContext => Unit]

  def getModuleName = "JacksonModule"

  def version = JacksonModule.version

  def setupModule(context: SetupContext) { initializers result() foreach (_ apply context) }

  protected def +=(init: SetupContext => Unit): this.type = { initializers += init; this }
  protected def +=(ser: Serializers): this.type = this += (_ addSerializers ser)
  protected def +=(deser: Deserializers): this.type = this += (_ addDeserializers deser)
  protected def +=(typeMod: TypeModifier): this.type = this += (_ addTypeModifier typeMod)
  protected def +=(beanSerMod: BeanSerializerModifier): this.type = this += (_ addBeanSerializerModifier beanSerMod)

}