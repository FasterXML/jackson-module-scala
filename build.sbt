import sbtghactions.JavaSpec.Distribution.Zulu
import com.github.sbt.sbom._
import xerial.sbt.Sonatype.sonatypeCentralHost

// Basic facts
name := "jackson-module-scala"

organization := "tools.jackson.module"

ThisBuild / version := "3.0.0-rc5-SNAPSHOT"

val scala213Version = "2.13.16"
ThisBuild / scalaVersion := scala213Version

ThisBuild / crossScalaVersions := Seq("2.12.20", scala213Version, "3.3.6")

ThisBuild / sonatypeCredentialHost := sonatypeCentralHost
// ThisBuild / sonatypeRepository := "s01.oss.sonatype.org"

resolvers += "Sonatype Central Snapshots" at "https://central.sonatype.com/repository/maven-snapshots"
resolvers ++= Resolver.sonatypeOssRepos("snapshots")

bomFormat := "xml"

val jacksonVersion = "3.0.0-rc5-SNAPSHOT"

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

libraryDependencies ++= Seq(
  "tools.jackson.core" % "jackson-core" % jacksonVersion changing(),
  "tools.jackson.core" % "jackson-databind" % jacksonVersion changing(),
  "tools.jackson.datatype" % "jackson-datatype-joda" % jacksonVersion % Test changing(),
  "tools.jackson.datatype" % "jackson-datatype-guava" % jacksonVersion % Test changing(),
  "org.scala-lang.modules" %% "scala-java8-compat" % "1.0.2" % Test,
  "tools.jackson.jaxrs" % "jackson-jaxrs-json-provider" % jacksonVersion % Test changing(),
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
ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec(Zulu, "17"), JavaSpec(Zulu, "21"))
ThisBuild / githubWorkflowTargetBranches := Seq("3.x")
ThisBuild / githubWorkflowTargetTags ++= Seq("v*")
ThisBuild / githubWorkflowPublishTargetBranches :=
  Seq(
    RefPredicate.StartsWith(Ref.Tag("v")),
    RefPredicate.Equals(Ref.Branch("3.x"))
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
