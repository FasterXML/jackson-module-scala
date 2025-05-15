package tools.jackson.module.scala

import tools.jackson.core.util.VersionUtil
import tools.jackson.core.{JsonParser, Version}
import tools.jackson.databind.DatabindException
import tools.jackson.databind.JacksonModule.SetupContext
import tools.jackson.databind.`type`.TypeModifier
import tools.jackson.databind.deser.{Deserializers, KeyDeserializers}
import tools.jackson.databind.ser.{Serializers, ValueSerializerModifier}

import java.util.Properties
import scala.collection.JavaConverters._

object JacksonModule {
  private val cls = classOf[JacksonModule]
  private val buildPropsFilename = cls.getPackage.getName.replace('.','/') + "/build.properties"
  lazy val buildProps: scala.collection.mutable.Map[String, String] = {
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

  class InitializerBuilder {
    private val initializers = Seq.newBuilder[SetupContext => Unit]
    def +=(init: SetupContext => Unit): this.type = {
      initializers += init
      this
    }
    def +=(ser: Serializers): this.type = this += { context =>
      context.addSerializers(ser)
    }
    def addKeySerializers(ser: Serializers): this.type = this += { context =>
      context.addKeySerializers(ser)
    }
    def +=(deser: Deserializers): this.type = this += { context =>
      context.addDeserializers(deser)
    }
    def +=(deser: KeyDeserializers): this.type = this += { context =>
      context.addKeyDeserializers(deser)
    }
    def +=(typeMod: TypeModifier): this.type = this += { context =>
      context.addTypeModifier(typeMod)
    }
    def +=(beanSerMod: ValueSerializerModifier): this.type = this += { context =>
      context.addSerializerModifier(beanSerMod)
    }
    def build(): Seq[SetupContext => Unit] = initializers.result()
  }
}

object VersionExtractor {
  def unapply(v: Version): Option[(Int, Int)] = Some(v.getMajorVersion, v.getMinorVersion)
}

trait JacksonModule extends tools.jackson.databind.JacksonModule {

  override def version(): Version = JacksonModule.version

  def setupModule(context: SetupContext): Unit = {
    val MajorVersion = JacksonModule.version.getMajorVersion
    val MinorVersion = JacksonModule.version.getMinorVersion

    val requiredVersion = new Version(MajorVersion, MinorVersion, 0, null, "tools.jackson.core", "jackson-databind")
    val incompatibleVersion = new Version(MajorVersion, MinorVersion + 1, 0, null, "tools.jackson.core", "jackson-databind")

    // Because of the Scala module's dependency on databind internals,
    // major and minor versions must match exactly.
    context.getMapperVersion match {
      case VersionExtractor(MajorVersion, MinorVersion) =>
        // success!
      case databindVersion =>
        val databindVersionError = "Scala module %s requires Jackson Databind version >= %s and < %s - Found jackson-databind version %s"
          .format(version, requiredVersion, incompatibleVersion, databindVersion)
        throw DatabindException.from(null.asInstanceOf[JsonParser], databindVersionError)
    }

    getInitializers(config).map { initFunction =>
      initFunction(context)
    }
  }

  protected def config: ScalaModule.Config = ScalaModule.defaultBuilder

  def getInitializers(config: ScalaModule.Config): Seq[SetupContext => Unit]
}
