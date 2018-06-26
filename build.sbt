import java.io.File

// Basic facts
name := "jackson-module-scala"

organization := "com.fasterxml.jackson.module"

scalaVersion := "2.12.6"

crossScalaVersions := Seq("2.10.7", "2.11.12", "2.12.6", "2.13.0-M4")

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

val myJavacOptions = SettingKey[Any]("myJavacOptions")
myJavacOptions := {
  if (scalaMajorVersion.value >= 12) {
    // -target is deprecated as of Scala 2.12, which uses JVM 1.8 bytecode
  } else {
    // Explicitly target 1.7 for scala < 2.12
    scalacOptions ++= Seq("-target:jvm-1.7")

    lazy val java7Home =
      Option(System.getenv("JAVA7_HOME"))
        .orElse(Option(System.getProperty("java7.home")))
        .map(new File(_))
        .getOrElse { sys.error("Please set JAVA7_HOME environment variable or java7.home system property") }

    javacOptions ++= Seq(
      "-source", "1.7",
      "-target", "1.7",
      "-bootclasspath", Array((java7Home / "jre" / "lib" / "rt.jar").toString, (java7Home / ".." / "Classes"/ "classes.jar").toString).mkString(File.pathSeparator)
    )
  }
}

val myScalaSource = SettingKey[Any]("myScalaSource")
myScalaSource := {
  if (scalaMajorVersion.value < 13) {
    unmanagedSourceDirectories in Compile ++= Seq(
      (baseDirectory in LocalRootProject).value / "src" / "main" / s"scala-2.13-"
    )
  }
}

val jacksonVersion = "2.9.6"

libraryDependencies ++= Seq(
    "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion,
    "com.fasterxml.jackson.core" % "jackson-annotations" % jacksonVersion,
    "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
    "com.fasterxml.jackson.module" % "jackson-module-paranamer" % jacksonVersion,
    // test dependencies
    "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % jacksonVersion % "test",
    "com.fasterxml.jackson.datatype" % "jackson-datatype-guava" % jacksonVersion % "test",
    "com.fasterxml.jackson.module" % "jackson-module-jsonSchema" % jacksonVersion % "test",
    "org.scalatest" %% "scalatest" % "3.0.6-SNAP1" % "test",
    "junit" % "junit" % "4.12" % "test"
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
