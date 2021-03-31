package com.fasterxml.jackson.module.scala

import java.util.Properties

import com.fasterxml.jackson.core.Version
import com.fasterxml.jackson.core.util.VersionUtil
import com.fasterxml.jackson.databind.Module.SetupContext
import com.fasterxml.jackson.databind.`type`.TypeModifier
import com.fasterxml.jackson.databind.deser.Deserializers
import com.fasterxml.jackson.databind.ser.{BeanSerializerModifier, Serializers}
import com.fasterxml.jackson.databind.{JsonMappingException, Module}

import scala.collection.JavaConverters._
import scala.collection.mutable

object JacksonModule {
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
    val version = buildProps("version")
    VersionUtil.parseVersion(version, groupId, artifactId)
  }
}

object VersionExtractor {
  def unapply(v: Version) = Some(v.getMajorVersion, v.getMinorVersion)
}

trait JacksonModule extends Module {

  private val initializers = Seq.newBuilder[SetupContext => Unit]

  def getModuleName = "JacksonModule"

  def version = JacksonModule.version

  def setupModule(context: SetupContext): Unit = {
    val MajorVersion = version.getMajorVersion
    val MinorVersion = version.getMinorVersion

    val requiredVersion = new Version(MajorVersion, MinorVersion, 0, null, "com.fasterxml.jackson.core", "jackson-databind")
    val incompatibleVersion = new Version(MajorVersion, MinorVersion + 1, 0, null, "com.fasterxml.jackson.core", "jackson-databind")
    
    // Because of the Scala module's dependency on databind internals,
    // major and minor versions must match exactly.
    context.getMapperVersion match {
      case VersionExtractor(MajorVersion, MinorVersion) =>
        // success!
      case databindVersion =>
        val databindVersionError = "Scala module %s requires Jackson Databind version >= %s and < %s - Found jackson-databind version %s"
          .format(version, requiredVersion, incompatibleVersion, databindVersion)
        throw new JsonMappingException(null, databindVersionError)
    }

    initializers.result().foreach(_ apply context)
  }

  protected def +=(init: SetupContext => Unit): this.type = { initializers += init; this }
  protected def +=(ser: Serializers): this.type = this += (_ addSerializers ser)
  protected def +=(deser: Deserializers): this.type = this += (_ addDeserializers deser)
  protected def +=(typeMod: TypeModifier): this.type = this += (_ addTypeModifier typeMod)
  protected def +=(beanSerMod: BeanSerializerModifier): this.type = this += (_ addBeanSerializerModifier beanSerMod)
}
