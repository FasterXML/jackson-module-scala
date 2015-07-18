import java.io.File

// Basic facts
name := "jackson-module-scala"

organization := "com.fasterxml.jackson.module"

scalaVersion := "2.11.7"

crossScalaVersions := Seq("2.10.5", "2.11.7")

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature")

scalacOptions in (Compile, compile) += "-Xfatal-warnings"

// Ensure jvm 1.6 for java
lazy val java6Home = new File(System.getenv("JAVA6_HOME"))

javacOptions ++= Seq(
  "-source", "1.6",
  "-target", "1.6",
  "-bootclasspath", Array((java6Home / "jre" / "lib" / "rt.jar").toString, (java6Home / ".." / "Classes"/ "classes.jar").toString).mkString(File.pathSeparator)
)

// Try to future-proof scala jvm targets, in case some future scala version makes 1.7 a default
scalacOptions += "-target:jvm-1.6"

libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    "com.fasterxml.jackson.core" % "jackson-core" % "2.6.0-rc4",
    "com.fasterxml.jackson.core" % "jackson-annotations" % "2.6.0-rc4",
    "com.fasterxml.jackson.core" % "jackson-databind" % "2.6.0-rc5-SNAPSHOT",
    "com.thoughtworks.paranamer" % "paranamer" % "2.6",
    // test dependencies
    "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % "2.6.0-rc4" % "test",
    "com.fasterxml.jackson.datatype" % "jackson-datatype-guava" % "2.6.0-rc4" % "test",
    "com.fasterxml.jackson.module" % "jackson-module-jsonSchema" % "2.6.0-rc4" % "test",
    "org.scalatest" %% "scalatest" % "2.2.1" % "test",
    "junit" % "junit" % "4.11" % "test"
)

// build.properties
resourceGenerators in Compile <+=
  (resourceManaged in Compile, version, organization, name) map { (dir, v, o, n) =>
    val file = dir / "com" / "fasterxml" / "jackson" / "module" / "scala" / "build.properties"
    val contents = "version=%s\ngroupId=%s\nartifactId=%s\n".format(v, o, n)
    IO.write(file, contents)
    Seq(file)
  }

// site
site.settings

site.includeScaladoc()

ghpages.settings

git.remoteRepo := "git@github.com:FasterXML/jackson-module-scala.git"
