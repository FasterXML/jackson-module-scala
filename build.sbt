import com.typesafe.tools.mima.core._
import sbtghactions.JavaSpec.Distribution.Zulu

// Basic facts
name := "jackson-module-scala"

organization := "com.fasterxml.jackson.module"

ThisBuild / scalaVersion := "2.13.10"

ThisBuild / crossScalaVersions := Seq("2.11.12", "2.12.15", "2.13.10", "3.0.2")

resolvers ++= Resolver.sonatypeOssRepos("snapshots")

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

Test / unmanagedSourceDirectories ++= {
  if (scalaReleaseVersion.value > 2) {
    Seq(
      (LocalRootProject / baseDirectory).value / "src" / "test" / "scala-3.0"
    )
  } else {
    Seq(
      (LocalRootProject / baseDirectory).value / "src" / "test" / s"scala-2.+",
      (LocalRootProject / baseDirectory).value / "src" / "test" / s"scala-2.${scalaMajorVersion.value}"
    )
  }
}

val jacksonVersion = "2.13.5-SNAPSHOT"
val jacksonJsonSchemaVersion = "2.13.4"

libraryDependencies ++= Seq(
  "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-annotations" % jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
  "com.thoughtworks.paranamer" % "paranamer" % "2.8",
  // test dependencies
  "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % jacksonVersion % Test,
  "com.fasterxml.jackson.datatype" % "jackson-datatype-guava" % jacksonVersion % Test,
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8" % jacksonVersion % Test,
  "com.fasterxml.jackson.jaxrs" % "jackson-jaxrs-json-provider" % jacksonVersion % Test,
  "com.fasterxml.jackson.module" % "jackson-module-jsonSchema" % jacksonJsonSchemaVersion % Test,
  "javax.ws.rs" % "javax.ws.rs-api" % "2.1.1" % Test,
  "io.swagger" % "swagger-core" % "1.6.3" % Test,
  "org.scalatest" %% "scalatest" % "3.2.11" % Test
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
ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec(Zulu, "8"), JavaSpec(Zulu, "11"), JavaSpec(Zulu, "17"))
ThisBuild / githubWorkflowBuild := Seq(WorkflowStep.Sbt(List("test", "mimaReportBinaryIssues")))
ThisBuild / githubWorkflowTargetTags ++= Seq("v*")
ThisBuild / githubWorkflowPublishTargetBranches := Seq(
  RefPredicate.Equals(Ref.Branch("master")),
  RefPredicate.Equals(Ref.Branch("2.14")),
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
      "SONATYPE_USERNAME" -> "${{ secrets.CI_DEPLOY_USERNAME }}",
      "CI_SNAPSHOT_RELEASE" -> "+publishSigned"
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
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.fasterxml.jackson.module.scala.introspect.PropertyDescriptor.findAnnotation"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.fasterxml.jackson.module.scala.ser.MapSerializerResolver.BASE"),
  ProblemFilters.exclude[ReversedMissingMethodProblem]("com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule._descriptorCache"),
  ProblemFilters.exclude[ReversedMissingMethodProblem]("com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule._descriptorCache_="),
  ProblemFilters.exclude[ReversedMissingMethodProblem]("com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule.setDescriptorCache"),
  ProblemFilters.exclude[ReversedMissingMethodProblem]("com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule.overrideMap"),
  ProblemFilters.exclude[ReversedMissingMethodProblem]("com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule.com$fasterxml$jackson$module$scala$introspect$ScalaAnnotationIntrospectorModule$_setter_$overrideMap_="),
  ProblemFilters.exclude[ReversedMissingMethodProblem]("com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule.clearRegisteredReferencedTypes"),
  ProblemFilters.exclude[ReversedMissingMethodProblem]("com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule.registerReferencedValueType"),
  ProblemFilters.exclude[ReversedMissingMethodProblem]("com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule.getRegisteredReferencedValueType"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospector#ScalaValueInstantiator.this")
)
