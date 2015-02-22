name := "jackson-module-scala-common"

scalaVersion := "2.11.5"

crossScalaVersions := Seq("2.10.4", "2.11.5")

libraryDependencies ++= Seq(
  // "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  "com.fasterxml.jackson.core" % "jackson-core" % "2.6.0-SNAPSHOT",
  "com.fasterxml.jackson.core" % "jackson-annotations" % "2.5.0",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.6.0-SNAPSHOT",
  "com.thoughtworks.paranamer" % "paranamer" % "2.6",
  // test dependencies
  "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % "2.5.0" % "test",
  "com.fasterxml.jackson.datatype" % "jackson-datatype-guava" % "2.5.0" % "test",
  "com.fasterxml.jackson.module" % "jackson-module-jsonSchema" % "2.5.0" % "test",
  "org.scalatest" %% "scalatest" % "2.2.1" % "test",
  "junit" % "junit" % "4.11" % "test"
)

// build.properties
resourceGenerators in Compile <+=
  (resourceManaged in Compile, version) map { (dir, v) =>
    val file = dir / "com" / "fasterxml" / "jackson" / "module" / "scala" / "build.properties"
    val contents = "version=%s\ngroupId=com.fasterxml.jackson.module\nartifactId=jackson-module-scala\n".format(v)
    IO.write(file, contents)
    Seq(file)
  }

// site
site.settings

site.includeScaladoc()

ghpages.settings

git.remoteRepo := "git@github.com:FasterXML/jackson-module-scala.git"

ReleaseKeys.crossBuild := true
