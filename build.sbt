import PgpKeys._
import ReleaseKeys._
import sbtrelease._
import ReleaseStateTransformations._

// Basic facts
name := "jackson-module-scala"

organization := "com.fasterxml.jackson.module"

crossScalaVersions := Seq("2.9.1", "2.9.2", "2.9.3", "2.10.0")

// For Jackson snapshots
resolvers += "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies <++= (version) { (v) => Seq(
    "com.fasterxml.jackson.core" % "jackson-core" % v % "provided",
    "com.fasterxml.jackson.core" % "jackson-annotations" % v % "provided",
    "com.fasterxml.jackson.core" % "jackson-databind" % v % "provided"
) }

libraryDependencies ++= Seq(
    "com.thoughtworks.paranamer" % "paranamer" % "2.3",
    "com.google.code.findbugs" % "jsr305" % "2.0.1",
    "com.google.guava" % "guava" % "13.0.1",
    // test dependencies
    "org.scalatest" %% "scalatest" % "2.0.M5b" % "test",
    "junit" % "junit" % "4.11" % "test",
    "com.novocode" % "junit-interface" % "0.10-M3" % "test",
    "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % "2.1.2" % "test"
)

// publishing
publishMavenStyle := true

publishTo <<= version { v =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra := {
  <url>http://wiki.fasterxml.com/JacksonModuleScala</url>
  <licenses>
    <license>
      <name>The Apache Software License, Version 2.0</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <connection>scm:git:git@github.com:FasterXML/jackson-module-scala.git</connection>
    <developerConnection>scm:git:git@github.com:FasterXML/jackson-module-scala.git</developerConnection>
    <url>http://github.com/FasterXML/jackson-module-scala</url>
  </scm>
  <developers>
    <developer>
      <id>tatu</id>
      <name>Tatu Saloranta</name>
      <email>tatu@fasterxml.com</email>
    </developer>
    <developer>
      <id>christopher</id>
      <name>Christopher Currie</name>
      <email>christopher@currie.com</email>
    </developer>
  </developers>
  <contributors>
    <contributor>
      <name>Nathaniel Bauernfeind</name>
    </contributor>
    <contributor>
      <name>Anton Panasenko</name>
    </contributor>
  </contributors>
}

// release
releaseSettings

// bump bugfix on release
nextVersion := { ver => Version(ver).map(_.bumpBugfix.asSnapshot.string).getOrElse(versionFormatError) }

// use maven style tag name
tagName <<= (name, version in ThisBuild) map { (n,v) => n + "-" + v }

// don't publish on release, it doesn't work with cross version publishing
// TODO: see about defining multiple release processes, to do prepare/perform
// split processes similar to the Maven release plugin
releaseProcess := Seq(
  // TODO remove when scalabeans isn't a custom build
  // checkSnapshotDependencies,
  inquireVersions,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  setNextVersion,
  commitNextVersion
)
