import java.io.File

// Basic facts
name := "jackson-module-scala"

organization := "com.fasterxml.jackson.module"

scalaVersion := "2.12.4"

crossScalaVersions := Seq("2.10.6", "2.11.11", "2.12.4", "2.13.0-M2")

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature")

// Temporarily disable warnings as error since SerializationFeature.WRITE_NULL_MAP_VALUES has been deprecated
// and we use it.
//scalacOptions in (Compile, compile) += "-Xfatal-warnings"


// Ensure jvm 1.7 for java
lazy val java7Home =
  Option(System.getenv("JAVA7_HOME"))
    .orElse(Option(System.getProperty("java7.home")))
    .map(new File(_))
    .getOrElse { sys.error("Please set JAVA7_HOME environment variable or java7.home system property") }

javacOptions ++= javaOptionsFor(scalaVersion.value)

def javaOptionsFor(version: String) =
  if (version.startsWith("2.12")) {
    Seq(
      "-source", "1.7",
      "-target", "1.7",
      // Because of 2.12, the build has to be driven with Java 8, but we should still do the right thing for
      // the Java 7 bootclasspath to prevent standard library incompatibilities (and eliminate build warning)
      "-bootclasspath", Array((java7Home / "jre" / "lib" / "rt.jar").toString, (java7Home / ".." / "Classes"/ "classes.jar").toString).mkString(File.pathSeparator)
    )
  } else {
    Seq(
      "-source", "1.8",
      "-target", "1.8")
  }

def scalaOptionsFor(version: String) =
  if (version.startsWith("2.11")) {
    // Explicitly target 1.6 for scala < 2.12
    Seq("-target:jvm-1.7")
  } else {
    // -target is deprecated as of Scala 2.12, which uses JVM 1.8 bytecode
    Seq.empty
  }

scalacOptions ++= (
  scalaOptionsFor(scalaVersion.value)
  )

val jacksonVersion = "2.9.4"

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-annotations" % jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
  "com.fasterxml.jackson.module" % "jackson-module-paranamer" % jacksonVersion,
  // test dependencies
  "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % jacksonVersion % "test",
  "com.fasterxml.jackson.datatype" % "jackson-datatype-guava" % jacksonVersion % "test",
  "com.fasterxml.jackson.module" % "jackson-module-jsonSchema" % jacksonVersion % "test",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "junit" % "junit" % "4.11" % "test"
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
