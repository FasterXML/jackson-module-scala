import java.io.File
import com.typesafe.tools.mima.core._

// Basic facts
name := "jackson-module-scala"

organization := "com.fasterxml.jackson.module"

scalaVersion := "2.10.7"

crossScalaVersions := Seq("2.10.7", "2.11.12", "2.12.13", "2.13.4")

mimaPreviousArtifacts := Set.empty

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

val jacksonVersion = "2.12.7"
val jacksonDatabindVersion = "2.12.7.1"

libraryDependencies ++= Seq(
  "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-annotations" % jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-databind" % jacksonDatabindVersion,
  "com.thoughtworks.paranamer" % "paranamer" % "2.8",
  // test dependencies
  "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % jacksonVersion % Test,
  "com.fasterxml.jackson.datatype" % "jackson-datatype-guava" % jacksonVersion % Test,
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8" % jacksonVersion % Test,
  "com.fasterxml.jackson.module" % "jackson-module-jsonSchema" % jacksonVersion % Test,
  "io.swagger" % "swagger-core" % "1.6.2" % Test,
  "org.scalatest" %% "scalatest" % "3.2.9" % Test
)

// build.properties
resourceGenerators in Compile += Def.task {
    val file = (resourceManaged in Compile).value / "com" / "fasterxml" / "jackson" / "module" / "scala" / "build.properties"
    val contents = "version=%s\ngroupId=%s\nartifactId=%s\n".format(version.value, organization.value, name.value)
    IO.write(file, contents)
    Seq(file)
}.taskValue

// site
enablePlugins(SiteScaladocPlugin)
enablePlugins(GhpagesPlugin)
git.remoteRepo := "git@github.com:FasterXML/jackson-module-scala.git"

mimaBinaryIssueFilters ++= Seq(
  ProblemFilters.exclude[ReversedMissingMethodProblem]("com.fasterxml.jackson.module.scala.util.ClassW.isScalaObject")
)

