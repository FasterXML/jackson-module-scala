import com.github.sbt.sbom._
import com.typesafe.tools.mima.core._
import sbtghactions.JavaSpec.Distribution.Zulu
import xerial.sbt.Sonatype.sonatypeCentralHost

// Basic facts
name := "jackson-module-scala"

organization := "com.fasterxml.jackson.module"

val scala213Version = "2.13.16"
ThisBuild / scalaVersion := scala213Version

ThisBuild / crossScalaVersions := Seq("2.11.12", "2.12.20", scala213Version, "3.3.6")

ThisBuild / sonatypeCredentialHost := sonatypeCentralHost

ThisBuild / dynverSonatypeSnapshots := true
ThisBuild / publishTo := {
  val centralSnapshots = "https://central.sonatype.com/repository/maven-snapshots/"
  if (isSnapshot.value) Some("central-snapshots" at centralSnapshots)
  else localStaging.value
}

bomFormat := "xml"

resolvers += Resolver.sonatypeCentralSnapshots

// ThisBuild / version := "2.20.2"
val jacksonAnnotationVersion = "2.20"
val jacksonCoreVersion = "2.20.2"
val jacksonNonCoreVersion = jacksonCoreVersion

autoAPIMappings := true

apiMappings ++= {
  def mappingsFor(organization: String, names: List[String], location: String, revision: (String) => String = identity): Seq[(File, URL)] =
    for {
      entry: Attributed[File] <- (Compile / fullClasspath).value
      module: ModuleID <- entry.get(moduleID.key)
      if module.organization == organization
      if names.exists(module.name.startsWith)
    } yield entry.data -> url(location.format(revision(module.revision)))

  val mappings: Seq[(File, URL)] =
    mappingsFor("org.scala-lang", List("scala-library"), "https://scala-lang.org/api/%s/") ++
      mappingsFor("com.fasterxml.jackson.core", List("jackson-core"), "https://javadoc.io/doc/com.fasterxml.jackson.core/jackson-core/%s/") ++
      mappingsFor("com.fasterxml.jackson.core", List("jackson-databind"), "https://javadoc.io/doc/com.fasterxml.jackson.core/jackson-databind/%s/")

  mappings.toMap
}

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

val addJava17Tests: Boolean = System.getProperty("java.specification.version").toDouble >= 17

mimaPreviousArtifacts := Set(organization.value %% name.value % "2.18.0")

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

Compile / compileOrder := CompileOrder.Mixed
Test / compileOrder := CompileOrder.JavaThenScala

Compile / unmanagedSourceDirectories ++= {
  if (scalaReleaseVersion.value > 2) {
    Seq(
      (LocalRootProject / baseDirectory).value / "src" / "main" / "scala-2.13",
      (LocalRootProject / baseDirectory).value / "src" / "main" / "scala-3"
    )
  } else {
    Seq(
      (LocalRootProject / baseDirectory).value / "src" / "main" / "scala-2.+",
      (LocalRootProject / baseDirectory).value / "src" / "main" / s"scala-2.${scalaMajorVersion.value}"
    )
  }
}

Test / unmanagedSourceDirectories ++= {
  if (scalaReleaseVersion.value > 2) {
    Seq(
      (LocalRootProject / baseDirectory).value / "src" / "test" / "scala-3"
    )
  } else {
    Seq(
      (LocalRootProject / baseDirectory).value / "src" / "test" / s"scala-2.+",
      (LocalRootProject / baseDirectory).value / "src" / "test" / s"scala-2.${scalaMajorVersion.value}"
    )
  }
}

Test / unmanagedSourceDirectories ++= {
  if (addJava17Tests) {
    Seq(
      (LocalRootProject / baseDirectory).value / "src" / "test" / "java-17",
      (LocalRootProject / baseDirectory).value / "src" / "test" / "scala-jdk-17",
    )
  } else {
    Seq.empty
  }
}

val jacksonDependencies = if (jacksonCoreVersion.contains("SNAPSHOT"))
  Seq(
    "com.fasterxml.jackson.core" % "jackson-core" % jacksonCoreVersion changing(),
    "com.fasterxml.jackson.core" % "jackson-annotations" % jacksonAnnotationVersion changing(),
    "com.fasterxml.jackson.core" % "jackson-databind" % jacksonCoreVersion changing()
  )
else
  Seq(
    "com.fasterxml.jackson.core" % "jackson-core" % jacksonCoreVersion,
    "com.fasterxml.jackson.core" % "jackson-annotations" % jacksonAnnotationVersion,
    "com.fasterxml.jackson.core" % "jackson-databind" % jacksonCoreVersion
  )

libraryDependencies ++= jacksonDependencies ++ Seq(
  "com.thoughtworks.paranamer" % "paranamer" % "2.8.3",
  // test dependencies
  "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % jacksonNonCoreVersion % Test,
  "com.fasterxml.jackson.datatype" % "jackson-datatype-guava" % jacksonNonCoreVersion % Test,
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8" % jacksonNonCoreVersion % Test,
  "com.fasterxml.jackson.jaxrs" % "jackson-jaxrs-json-provider" % jacksonNonCoreVersion % Test,
  "com.fasterxml.jackson.module" % "jackson-module-jsonSchema" % jacksonNonCoreVersion % Test,
  "javax.ws.rs" % "javax.ws.rs-api" % "2.1.1" % Test,
  "io.swagger" % "swagger-core" % "1.6.8" % Test,
  "org.scalatest" %% "scalatest" % "3.2.19" % Test
)

// build.properties
Compile / resourceGenerators += Def.task {
    val file = (Compile / resourceManaged).value / "com" / "fasterxml" / "jackson" / "module" / "scala" / "build.properties"
    val contents = "version=%s\ngroupId=%s\nartifactId=%s\n".format(version.value, organization.value, name.value)
    IO.write(file, contents)
    Seq(file)
}.taskValue

Test / parallelExecution := false

ThisBuild / githubWorkflowSbtCommand := "sbt -J-Xmx2G"
ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec(Zulu, "8"), JavaSpec(Zulu, "11"),
  JavaSpec(Zulu, "17"), JavaSpec(Zulu, "21"))
ThisBuild / githubWorkflowBuild := Seq(WorkflowStep.Sbt(List("test", "mimaReportBinaryIssues")))
ThisBuild / githubWorkflowTargetBranches := Seq("2.*")
ThisBuild / githubWorkflowTargetTags ++= Seq("v*")
ThisBuild / githubWorkflowPublishTargetBranches := Seq(
  RefPredicate.StartsWith(Ref.Branch("2.")),
  RefPredicate.StartsWith(Ref.Tag("v"))
)

ThisBuild / githubWorkflowPublish := Seq(
  WorkflowStep.Sbt(
    List("ci-release"),
    env = Map(
      "PGP_PASSPHRASE" -> "${{ secrets.PGP_PASSPHRASE }}",
      "PGP_SECRET" -> "${{ secrets.PGP_SECRET }}",
      "SONATYPE_PASSWORD" -> "${{ secrets.CENTRAL_DEPLOY_PASSWORD }}",
      "SONATYPE_USERNAME" -> "${{ secrets.CENTRAL_DEPLOY_USERNAME }}",
      "CI_SNAPSHOT_RELEASE" -> "+publishSigned"
    )
  )
)

enablePlugins(ReproducibleBuildsPlugin)
// site
enablePlugins(SiteScaladocPlugin)
//enablePlugins(GhpagesPlugin)
git.remoteRepo := "git@github.com:FasterXML/jackson-module-scala.git"

mimaBinaryIssueFilters ++= Seq(
  ProblemFilters.exclude[IncompatibleResultTypeProblem]("com.fasterxml.jackson.module.scala.deser.ImmutableBitSetDeserializer.getNullValue"),
  ProblemFilters.exclude[MissingTypesProblem]("com.fasterxml.jackson.module.scala.DefaultScalaModule"),
  ProblemFilters.exclude[MissingTypesProblem]("com.fasterxml.jackson.module.scala.DefaultScalaModule$")
)

