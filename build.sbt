import java.io.File

// Basic facts
name := "jackson-module-scala"

organization := "__foursquare_shaded__.com.fasterxml.jackson.module"

scalaVersion := "2.11.12"

crossScalaVersions := Seq("2.10.7", "2.11.12", "2.12.10", "2.13.1")

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

val jacksonVersion = "2.9.10"

libraryDependencies ++= Seq(
  // test dependencies
  "com.google.guava" % "guava" % "18.0" % "test",
  "joda-time" % "joda-time" % "2.7" % "test",
  "org.scalatest" %% "scalatest" % "3.0.8" % "test",
  "junit" % "junit" % "4.12" % "test"
)

// NOTE(jacob): If re-patching, you will need to drop copies of our shaded jackson jar
//    into the repo's top-level 'lib' directory for sbt to find them.
unmanagedBase := file("lib")

excludeDependencies ++= Seq(
  // provided by shaded jackson fat jars
  SbtExclusionRule("com.fasterxml.jackson.core", "jackson-core"),
  SbtExclusionRule("com.fasterxml.jackson.core", "jackson-annotations"),
  SbtExclusionRule("com.fasterxml.jackson.core", "jackson-databind"),
  SbtExclusionRule("com.fasterxml.jackson.datatype", "jackson-datatype-guava"),
  SbtExclusionRule("com.fasterxml.jackson.datatype", "jackson-datatype-joda"),
  SbtExclusionRule("com.fasterxml.jackson.module", "jackson-module-jsonSchema"),
  SbtExclusionRule("com.fasterxml.jackson.module", "jackson-module-paranamer")
)

// build.properties
resourceGenerators in Compile += Def.task {
  // NOTE(jacob): This file is read at runtime using the shaded package namespace, see
  //    src/main/scala/com/fasterxml/jackson/module/scala/JacksonModule.scala for details.
  val file = (resourceManaged in Compile).value / "__foursquare_shaded__" / "com" / "fasterxml" / "jackson" / "module" / "scala" / "build.properties"
  val contents = "version=%s\ngroupId=%s\nartifactId=%s\n".format(version.value, organization.value, name.value)
  IO.write(file, contents)
  Seq(file)
}.taskValue

// site
site.settings

site.includeScaladoc()

ghpages.settings

git.remoteRepo := "git@github.com:FasterXML/jackson-module-scala.git"
