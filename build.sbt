// Basic facts
name := "jackson-module-scala"

organization := "com.fasterxml.jackson.module"

scalaVersion := "2.10.3"

crossScalaVersions := Seq("2.10.3", "2.11.0-RC4")

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-Xfatal-warnings")

scalacOptions in Test := Seq("-deprecation", "-unchecked", "-feature")

// Ensure jvm 1.6 for java
javacOptions ++= Seq("-source", "1.6", "-target", "1.6")

// Try to future-proof scala jvm targets, in case some future scala version makes 1.7 a default
scalacOptions += "-target:jvm-1.6"

libraryDependencies ++= Seq(
    "com.fasterxml.jackson.core" % "jackson-core" % "2.4.0-SNAPSHOT",
    "com.fasterxml.jackson.core" % "jackson-annotations" % "2.4-SNAPSHOT",
    "com.fasterxml.jackson.core" % "jackson-databind" % "2.4.0-SNAPSHOT",
    "com.thoughtworks.paranamer" % "paranamer" % "2.6",
    "com.google.code.findbugs" % "jsr305" % "2.0.1",
    "com.google.guava" % "guava" % "15.0",
    // test dependencies
    "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % "2.3.1" % "test",
    "com.fasterxml.jackson.datatype" % "jackson-datatype-guava" % "2.3.1" % "test",
    "com.fasterxml.jackson.module" % "jackson-module-jsonSchema" % "2.3.1" % "test",
    "org.scalatest" %% "scalatest" % "2.1.3" % "test",
    "junit" % "junit" % "4.11" % "test"
)

// resource filtering
seq(filterSettings: _*)

