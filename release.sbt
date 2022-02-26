import com.typesafe.sbt.osgi.OsgiKeys

// OSGI bundles
lazy val jacksonOsgiSettings = osgiSettings ++ Seq(
  OsgiKeys.exportPackage := Seq("com.fasterxml.jackson.module.scala.*"),
  OsgiKeys.privatePackage := Seq(),
  OsgiKeys.additionalHeaders := Map("Automatic-Module-Name" -> "com.fasterxml.jackson.module.scala")
)

lazy val jacksonProject = project.in(file(".")).enablePlugins(SbtOsgi).settings(jacksonOsgiSettings:_*)

// publishing
Test / publishArtifact := false

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
    <developer>
      <id>pjfanning</id>
      <name>PJ Fanning</name>
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
