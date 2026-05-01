import sbtghactions.JavaSpec.Distribution.Zulu
import com.typesafe.tools.mima.core._
import com.github.sbt.sbom._
import xerial.sbt.Sonatype.sonatypeCentralHost

// Basic facts
name := "jackson-module-scala"

organization := "tools.jackson.module"

val scala213Version = "2.13.18"
ThisBuild / scalaVersion := scala213Version

ThisBuild / crossScalaVersions := Seq("2.12.21", scala213Version, "3.3.7")

ThisBuild / sonatypeCredentialHost := sonatypeCentralHost

ThisBuild / dynverSonatypeSnapshots := true
ThisBuild / publishTo := {
  val centralSnapshots = "https://central.sonatype.com/repository/maven-snapshots/"
  if (isSnapshot.value) Some("central-snapshots" at centralSnapshots)
  else localStaging.value
}

bomFormat := "xml"

resolvers += Resolver.sonatypeCentralSnapshots
ThisBuild / version := "3.1.4-SNAPSHOT"
val jacksonVersion = "3.1.4-SNAPSHOT"

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
      mappingsFor("tools.jackson.core", List("jackson-core"), "https://javadoc.io/doc/tools.jackson.core/jackson-core/%s/") ++
      mappingsFor("tools.jackson.core", List("jackson-databind"), "https://javadoc.io/doc/tools.jackson.core/jackson-databind/%s/")

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

mimaPreviousArtifacts := Set(organization.value %% name.value % "3.0.0")

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature")

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

val jacksonDependencies = if (jacksonVersion.contains("SNAPSHOT"))
  Seq(
    "tools.jackson.core" % "jackson-core" % jacksonVersion changing(),
    "tools.jackson.core" % "jackson-databind" % jacksonVersion changing(),
    "tools.jackson.datatype" % "jackson-datatype-joda" % jacksonVersion % Test changing(),
    "tools.jackson.datatype" % "jackson-datatype-guava" % jacksonVersion % Test changing(),
    "tools.jackson.jaxrs" % "jackson-jaxrs-json-provider" % jacksonVersion % Test changing()
  )
else
  Seq(
    "tools.jackson.core" % "jackson-core" % jacksonVersion,
    "tools.jackson.core" % "jackson-databind" % jacksonVersion,
    "tools.jackson.datatype" % "jackson-datatype-joda" % jacksonVersion % Test,
    "tools.jackson.datatype" % "jackson-datatype-guava" % jacksonVersion % Test,
    "tools.jackson.jaxrs" % "jackson-jaxrs-json-provider" % jacksonVersion % Test
  )

libraryDependencies ++= jacksonDependencies ++ Seq(
  "org.scala-lang.modules" %% "scala-java8-compat" % "1.0.2" % Test,
  "javax.ws.rs" % "javax.ws.rs-api" % "2.1.1" % Test,
  "io.swagger" % "swagger-core" % "1.6.8" % Test,
  "org.scalatest" %% "scalatest" % "3.2.19" % Test
)

// build.properties
Compile / resourceGenerators += Def.task {
    val file = (Compile / resourceManaged).value / "tools" / "jackson" / "module" / "scala" / "build.properties"
    val contents = "version=%s\ngroupId=%s\nartifactId=%s\n".format(version.value, organization.value, name.value)
    IO.write(file, contents)
    Seq(file)
}.taskValue

Test / parallelExecution := false

ThisBuild / githubWorkflowSbtCommand := "sbt -J-Xmx2G"
ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec(Zulu, "17"), JavaSpec(Zulu, "21"), JavaSpec(Zulu, "25"))
ThisBuild / githubWorkflowBuild := Seq(WorkflowStep.Sbt(List("test", "mimaReportBinaryIssues")))
ThisBuild / githubWorkflowTargetBranches := Seq("3.x", "3.1", "3.0")
ThisBuild / githubWorkflowTargetTags ++= Seq("v*")
ThisBuild / githubWorkflowPublishTargetBranches :=
  Seq(
    RefPredicate.StartsWith(Ref.Tag("v")),
    RefPredicate.StartsWith(Ref.Branch("3."))
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
  ProblemFilters.exclude[DirectMissingMethodProblem]("tools.jackson.module.scala.ser.IterableSerializer.withResolved"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("tools.jackson.module.scala.ser.ResolvedIterableSerializer.withResolved"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("tools.jackson.module.scala.ser.ResolvedIterableSerializer.this"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("tools.jackson.module.scala.ser.ResolvedIteratorSerializer.withResolved"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("tools.jackson.module.scala.ser.ResolvedIteratorSerializer.this"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("tools.jackson.module.scala.ser.ScalaIterableSerializer.*"),
  ProblemFilters.exclude[MissingTypesProblem]("tools.jackson.module.scala.ser.ScalaIterableSerializer$"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("tools.jackson.module.scala.ser.ScalaIteratorSerializer.*"),
  ProblemFilters.exclude[MissingTypesProblem]("tools.jackson.module.scala.ser.ScalaIteratorSerializer$"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("tools.jackson.module.scala.ser.UnresolvedIterableSerializer.withResolved"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("tools.jackson.module.scala.ser.UnresolvedIteratorSerializer.withResolved"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("tools.jackson.module.scala.ser.IteratorSerializer.withResolved")
)
