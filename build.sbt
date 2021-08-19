import java.io.File
import com.typesafe.tools.mima.core._

// Basic facts
name := "jackson-module-scala"

organization := "com.fasterxml.jackson.module"

ThisBuild / scalaVersion := "2.13.6"

ThisBuild / crossScalaVersions := Seq("2.11.12", "2.12.14", "2.13.6", "3.0.1")

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

mimaPreviousArtifacts := {
  if (scalaReleaseVersion.value > 2)
    Set.empty
  else
    Set(organization.value %% name.value % "2.12.1")
}

scalacOptions ++= {
  val additionalSettings =
    if (scalaReleaseVersion.value == 2 && scalaMajorVersion.value <= 12) {
      Seq("-language:higherKinds", "-language:existentials")
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

val jacksonVersion = "2.13.0-rc2-SNAPSHOT"
val jacksonJsonSchemaVersion = "2.13.0-rc1"

libraryDependencies ++= Seq(
  "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-annotations" % jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
  "com.thoughtworks.paranamer" % "paranamer" % "2.8",
  // test dependencies
  "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % jacksonVersion % Test,
  "com.fasterxml.jackson.datatype" % "jackson-datatype-guava" % jacksonVersion % Test,
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8" % jacksonVersion % Test,
  "com.fasterxml.jackson.module" % "jackson-module-jsonSchema" % jacksonJsonSchemaVersion % Test,
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

ThisBuild / githubWorkflowBuild := Seq(WorkflowStep.Sbt(List("test", "mimaReportBinaryIssues")))
ThisBuild / githubWorkflowTargetTags ++= Seq("v*")
ThisBuild / githubWorkflowPublishTargetBranches := Seq(
  RefPredicate.Equals(Ref.Branch("master")),
  RefPredicate.Equals(Ref.Branch("2.13")),
  RefPredicate.StartsWith(Ref.Tag("v"))
)

ThisBuild / githubWorkflowPublish := Seq(
  WorkflowStep.Sbt(
    List("ci-release"),
    env = Map(
      "PGP_PASSPHRASE" -> "${{ secrets.PGP_PASSPHRASE }}",
      "PGP_SECRET" -> "${{ secrets.PGP_SECRET }}",
      "SONATYPE_PASSWORD" -> "${{ secrets.CI_DEPLOY_PASSWORD }}",
      "SONATYPE_USERNAME" -> "${{ secrets.CI_DEPLOY_USERNAME }}"
    )
  )
)

// site
enablePlugins(SiteScaladocPlugin)
enablePlugins(GhpagesPlugin)
git.remoteRepo := "git@github.com:FasterXML/jackson-module-scala.git"

mimaBinaryIssueFilters ++= Seq(
  ProblemFilters.exclude[ReversedMissingMethodProblem]("com.fasterxml.jackson.module.scala.util.ClassW.isScalaObject"),
  ProblemFilters.exclude[IncompatibleResultTypeProblem]("com.fasterxml.jackson.module.scala.deser.UntypedObjectDeserializerResolver.findBeanDeserializer"),
  ProblemFilters.exclude[MissingClassProblem]("com.fasterxml.jackson.module.scala.deser.UntypedObjectDeserializer*"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.fasterxml.jackson.module.scala.introspect.BeanIntrospector.apply"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.fasterxml.jackson.module.scala.introspect.PropertyDescriptor.findAnnotation")
)
