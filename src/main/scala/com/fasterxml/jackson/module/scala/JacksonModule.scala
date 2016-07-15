package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.core.Version

import com.fasterxml.jackson.databind.{JsonMappingException, Module}
import com.fasterxml.jackson.databind.Module.SetupContext
import com.fasterxml.jackson.databind.deser.Deserializers
import com.fasterxml.jackson.databind.ser.{Serializers, BeanSerializerModifier}
import com.fasterxml.jackson.databind.`type`.TypeModifier

import java.util.Properties
import collection.JavaConverters._
import collection.mutable

object JacksonModule {
  private val VersionRegex = """(\d+)\.(\d+)(?:\.(\d+)(?:[-.]rc(?:\d+)*)?(?:\-(.*))?)?""".r
  private val cls = classOf[JacksonModule]
  private val buildPropsFilename = cls.getPackage.getName.replace('.','/') + "/build.properties"
  lazy val buildProps: mutable.Map[String, String] = {
    val props = new Properties
    val stream = cls.getClassLoader.getResourceAsStream(buildPropsFilename)
    if (stream ne null) props.load(stream)

    props.asScala
  }
  lazy val version: Version = {
    val groupId = buildProps("groupId")
    val artifactId = buildProps("artifactId")
    buildProps("version") match {
      case VersionRegex(major,minor,patchOpt,snapOpt) => {
        val patch = Option(patchOpt) map (_.toInt) getOrElse 0
        new Version(major.toInt,minor.toInt,patch,snapOpt,groupId,artifactId)
      }
      case _ => Version.unknownVersion()
    }
  }
}

object VersionExtractor {
  def unapply(v: Version) = Some(v.getMajorVersion, v.getMinorVersion)
}

trait JacksonModule extends Module {

  private val initializers = Seq.newBuilder[SetupContext => Unit]

  def getModuleName = "JacksonModule"

  def version = JacksonModule.version

  def setupModule(context: SetupContext) {
    val MajorVersion = version.getMajorVersion
    val MinorVersion = version.getMinorVersion
    context.getMapperVersion match {
      case version@VersionExtractor(MajorVersion, minor) if minor < MinorVersion =>
        throw new JsonMappingException(null, "Jackson version is too old " + version)
      case version@VersionExtractor(MajorVersion, minor) =>
        // Under semantic versioning, this check would not be needed; however Jackson
        // occasionally has functionally breaking changes across minor versions
        // (2.4 -> 2.5 as an example). This may be the fault of the Scala module
        // depending on implementation details, so for now we'll just declare ourselves
        // as incompatible and move on.
        if (minor > MinorVersion) {
          throw new JsonMappingException(null, "Incompatible Jackson version: " + version)
        }
      case version =>
        throw new JsonMappingException(null, "Incompatible Jackson version: " + version)
    }

    initializers result() foreach (_ apply context)
  }

  protected def +=(init: SetupContext => Unit): this.type = { initializers += init; this }
  protected def +=(ser: Serializers): this.type = this += (_ addSerializers ser)
  protected def +=(deser: Deserializers): this.type = this += (_ addDeserializers deser)
  protected def +=(typeMod: TypeModifier): this.type = this += (_ addTypeModifier typeMod)
  protected def +=(beanSerMod: BeanSerializerModifier): this.type = this += (_ addBeanSerializerModifier beanSerMod)

}
