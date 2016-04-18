import java.io.File

// Basic facts
name := "jackson-module-scala"

organization := "com.fasterxml.jackson.module"

scalaVersion := "2.11.8"

crossScalaVersions := Seq("2.10.6", "2.11.8", "2.12.0-M4")

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature")

// Temporarily disable warnings as fatal until migrate Option to new features.
//scalacOptions in (Compile, compile) += "-Xfatal-warnings"

// Ensure jvm 1.6 for java
lazy val java6Home = Option(System.getenv("JAVA6_HOME")).map(new File(_)).getOrElse {
  sys.error("Please set JAVA6_HOME environment variable")
}

javacOptions ++= Seq(
  "-source", "1.6",
  "-target", "1.6",
  "-bootclasspath", Array((java6Home / "jre" / "lib" / "rt.jar").toString, (java6Home / ".." / "Classes"/ "classes.jar").toString).mkString(File.pathSeparator)
)

scalacOptions ++= (
  if (scalaVersion.value.startsWith("2.12")) {
    // -target is deprecated as of Scala 2.12, which uses JVM 1.8 bytecode
    Seq.empty
  } else {
    // Explicitly target 1.6 for scala < 2.12
    Seq("-target:jvm-1.6")
  }
)

val jacksonVersion = "2.6.6"

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
    "org.scalatest" %% "scalatest" % "2.2.6" % "test",
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
