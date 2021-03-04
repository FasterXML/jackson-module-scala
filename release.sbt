import ReleaseTransformations._
import com.typesafe.sbt.osgi.OsgiKeys

// OSGI bundles
lazy val jacksonOsgiSettings = osgiSettings ++ Seq(
  OsgiKeys.exportPackage := Seq("com.fasterxml.jackson.module.scala.*"),
  OsgiKeys.privatePackage := Seq()
)

lazy val jacksonProject = project.in(file(".")).enablePlugins(SbtOsgi).settings(jacksonOsgiSettings:_*)

// publishing
publishMavenStyle := true

releaseCrossBuild := true

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials_sonatype")

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (version.value.trim.endsWith("SNAPSHOT"))
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
      <url>https://www.apache.org/licenses/LICENSE-2.0.txt</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
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
    <developer>
      <id>mbknor</id>
      <name>Morten Kjetland</name>
      <email>mbk@kjetland.com</email>
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

// use maven style tag name
releaseTagName := s"${name.value}-${(version in ThisBuild).value}"

// sign artifacts

releasePublishArtifactsAction := PgpKeys.publishSigned.value

// don't push changes (so they can be verified first)
releaseProcess := Seq(
  checkSnapshotDependencies,
  inquireVersions,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  publishArtifacts,
  setNextVersion,
  commitNextVersion,
  pushChanges,
  releaseStepCommand("sonatypeRelease")
)
