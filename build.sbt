import java.io.File

// Basic facts
name := "jackson-module-scala"

organization := "com.fasterxml.jackson.module"

scalaVersion := "2.12.11"

crossScalaVersions := Seq("2.10.7", "2.11.12", "2.12.11", "2.13.2")

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

val jacksonVersion = "2.11.0.rc1"

libraryDependencies ++= Seq(
    "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion,
    "com.fasterxml.jackson.core" % "jackson-annotations" % jacksonVersion,
    "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
    "com.fasterxml.jackson.module" % "jackson-module-paranamer" % jacksonVersion,
    // test dependencies
    "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % jacksonVersion % "test",
    "com.fasterxml.jackson.datatype" % "jackson-datatype-guava" % jacksonVersion % "test",
    "com.fasterxml.jackson.module" % "jackson-module-jsonSchema" % jacksonVersion % "test",
    "org.scalatest" %% "scalatest" % "3.0.8" % "test",
    "junit" % "junit" % "4.13" % "test"
)

// build.properties
resourceGenerators in Compile += Def.task {
    val file = (resourceManaged in Compile).value / "com" / "fasterxml" / "jackson" / "module" / "scala" / "build.properties"
    val contents = "version=%s\ngroupId=%s\nartifactId=%s\n".format(version.value, organization.value, name.value)
    IO.write(file, contents)
    Seq(file)
}.taskValue

// site
site.settings

site.includeScaladoc()

ghpages.settings

git.remoteRepo := "git@github.com:FasterXML/jackson-module-scala.git"
