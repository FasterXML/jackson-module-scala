import java.io.File

// Basic facts
name := "jackson-module-scala"

organization := "com.fasterxml.jackson.module"

scalaVersion := "2.13.4"

crossScalaVersions := Seq("2.11.12", "2.12.12", "2.13.4")

resolvers += Resolver.sonatypeRepo("snapshots")

val scalaMajorVersion = SettingKey[Int]("scalaMajorVersion")
scalaMajorVersion := {
  val v = scalaVersion.value
  CrossVersion.partialVersion(v).map(_._2.toInt).getOrElse {
    throw new RuntimeException(s"could not get Scala major version from $v")
  }
}

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature")

// Temporarily disable warnings as error since SerializationFeature.WRITE_NULL_MAP_VALUES has been deprecated
// and we use it.
//scalacOptions in (Compile, compile) += "-Xfatal-warnings"

unmanagedSourceDirectories in Compile += {
  (baseDirectory in LocalRootProject).value / "src" / "main" / s"scala-2.${scalaMajorVersion.value}"
}

val jacksonVersion = "2.12.2-SNAPSHOT"
val jacksonJsonSchemaVersion = "2.12.1"

libraryDependencies ++= Seq(
  "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-annotations" % jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
) ++ {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, scalaMajor)) if scalaMajor <= 11 =>
      Seq("com.thoughtworks.paranamer" % "paranamer" % "2.8")
    case _ => Seq.empty
  }
} ++ Seq(
  // test dependencies
  "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % jacksonVersion % Test,
  "com.fasterxml.jackson.datatype" % "jackson-datatype-guava" % jacksonVersion % Test,
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8" % jacksonVersion % Test,
  "com.fasterxml.jackson.module" % "jackson-module-jsonSchema" % jacksonJsonSchemaVersion % Test,
  "io.swagger" % "swagger-core" % "1.6.2" % Test,
  "org.scalatest" %% "scalatest" % "3.2.3" % Test
)

// build.properties
resourceGenerators in Compile += Def.task {
    val file = (resourceManaged in Compile).value / "com" / "fasterxml" / "jackson" / "module" / "scala" / "build.properties"
    val contents = "version=%s\ngroupId=%s\nartifactId=%s\n".format(version.value, organization.value, name.value)
    IO.write(file, contents)
    Seq(file)
}.taskValue

// site
Global / useGpg := false
enablePlugins(SiteScaladocPlugin)
enablePlugins(GhpagesPlugin)
git.remoteRepo := "git@github.com:FasterXML/jackson-module-scala.git"
