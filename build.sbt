import java.io.File

// Basic facts
name := "jackson-module-scala"

organization := "com.fasterxml.jackson.module"

scalaVersion := "2.13.6"

crossScalaVersions := Seq("2.11.12", "2.12.14", "2.13.6", "3.0.0")

resolvers += Resolver.sonatypeRepo("snapshots")

val scalaReleaseVersion = SettingKey[Int]("scalaReleaseVersion")
scalaReleaseVersion := {
  val v = scalaVersion.value
  CrossVersion.partialVersion(v).map(_._1.toInt).getOrElse {
    throw new RuntimeException(s"could not get Scala release version from $v")
  }
}

val scalaMajorVersion = SettingKey[Int]("scalaMajorVersion")
scalaMajorVersion := {
  val v = scalaVersion.value
  CrossVersion.partialVersion(v).map(_._2.toInt).getOrElse {
    throw new RuntimeException(s"could not get Scala major version from $v")
  }
}

scalacOptions ++= {
  val additionalSettings =
    if (scalaReleaseVersion.value == 2 && scalaMajorVersion.value <= 12) {
      Seq("-language:higherKinds")
    } else {
      Seq.empty[String]
    }
  Seq("-deprecation", "-unchecked", "-feature") ++ additionalSettings
}

// Temporarily disable warnings as error since SerializationFeature.WRITE_NULL_MAP_VALUES has been deprecated
// and we use it.
//scalacOptions in (Compile, compile) += "-Xfatal-warnings"

Compile / unmanagedSourceDirectories ++= {
  if (scalaReleaseVersion.value > 2) {
    Seq(
      (LocalRootProject / baseDirectory).value / "src" / "main" / "scala-2.13",
      (LocalRootProject / baseDirectory).value / "src" / "main" / "scala-3.0"
    )
  } else {
    Seq(
      (LocalRootProject / baseDirectory).value / "src" / "main" / "scala-2.+",
      (LocalRootProject / baseDirectory).value / "src" / "main" / s"scala-2.${scalaMajorVersion.value}"
    )
  }
}

Test / unmanagedSourceDirectories += {
  val suffix = if (scalaReleaseVersion.value > 2) "3.0" else "2.+"
  (LocalRootProject / baseDirectory).value / "src" / "test" / s"scala-${suffix}"
}

val jacksonVersion = "3.0.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion changing(),
  "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion changing(),
  "com.thoughtworks.paranamer" % "paranamer" % "2.8",
  "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % jacksonVersion % Test,
  "com.fasterxml.jackson.datatype" % "jackson-datatype-guava" % jacksonVersion % Test,
  "org.scala-lang.modules" %% "scala-java8-compat" % "1.0.0" % Test,
  "io.swagger" % "swagger-core" % "1.6.2" % Test,
  "org.scalatest" %% "scalatest" % "3.2.9" % Test
)

// build.properties
Compile / resourceGenerators += Def.task {
    val file = (Compile / resourceManaged).value / "com" / "fasterxml" / "jackson" / "module" / "scala" / "build.properties"
    val contents = "version=%s\ngroupId=%s\nartifactId=%s\n".format(version.value, organization.value, name.value)
    IO.write(file, contents)
    Seq(file)
}.taskValue

// site
enablePlugins(SiteScaladocPlugin)
enablePlugins(GhpagesPlugin)
git.remoteRepo := "git@github.com:FasterXML/jackson-module-scala.git"
