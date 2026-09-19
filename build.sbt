import sbtghactions.JavaSpec.Distribution.Zulu
import com.typesafe.tools.mima.core._
import com.github.sbt.sbom._
import xerial.sbt.Sonatype.sonatypeCentralHost

// Basic facts
name := "jackson-module-scala"

organization := "tools.jackson.module"

val scala213Version = "2.13.18"
ThisBuild / scalaVersion := scala213Version

ThisBuild / crossScalaVersions := Seq("2.12.21", scala213Version, "3.3.8")

ThisBuild / sonatypeCredentialHost := sonatypeCentralHost

ThisBuild / dynverSonatypeSnapshots := true
ThisBuild / publishTo := {
  val centralSnapshots = "https://central.sonatype.com/repository/maven-snapshots/"
  if (isSnapshot.value) Some("central-snapshots" at centralSnapshots)
  else localStaging.value
}

bomFormat := "xml"

resolvers += Resolver.sonatypeCentralSnapshots
ThisBuild / version := "3.3.0-SNAPSHOT"
val jacksonVersion = "3.3.0-SNAPSHOT"

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
    // as for main: a test that needs only the 2.13 collections library is written once, in scala-2.13
    Seq(
      (LocalRootProject / baseDirectory).value / "src" / "test" / "scala-2.13",
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
  "javax.ws.rs" % "javax.ws.rs-api" % "2.1.1" % Test,
  "io.swagger" % "swagger-core" % "1.6.8" % Test,
  "org.scalatest" %% "scalatest" % "3.2.20" % Test
)

// SealedPolymorphismSupport enumerates a sealed hierarchy through scala-reflect on Scala 2. It is
// optional: only applications that use that marker trait need it on the runtime classpath.
libraryDependencies ++= {
  if (scalaReleaseVersion.value == 2) Seq("org.scala-lang" % "scala-reflect" % scalaVersion.value % Optional)
  else Seq.empty
}

// build.properties
Compile / resourceGenerators += Def.task {
    val file = (Compile / resourceManaged).value / "tools" / "jackson" / "module" / "scala" / "build.properties"
    val contents = "version=%s\ngroupId=%s\nartifactId=%s\n".format(version.value, organization.value, name.value)
    IO.write(file, contents)
    Seq(file)
}.taskValue

Test / parallelExecution := false

ThisBuild / githubWorkflowSbtCommand := "sbt -J-Xmx2G"
// The Scala 3 artifact is built and published with the 3.3 LTS line, which is what
// crossScalaVersions says. CI also builds and tests with the latest Scala 3, which is kept
// out of crossScalaVersions so that a release does not try to publish a second `_3` artifact.
// A version that is not in crossScalaVersions has to be forced, hence the `!`.
val extraScalaVersionsForCI = Seq("3.9.0")
ThisBuild / githubWorkflowScalaVersions := (ThisBuild / crossScalaVersions).value ++ extraScalaVersionsForCI
ThisBuild / githubWorkflowBuildSbtStepPreamble := Seq("++ ${{ matrix.scala }}!")
// Forcing also sets ThisBuild / scalaVersion, which is where the publish job takes its one Scala
// version from, so the workflow the check step regenerates would differ from job to job. Pin it.
ThisBuild / githubWorkflowGeneratedCI ~= { jobs =>
  jobs.map(job => if (job.id == "publish") job.copy(scalas = List(scala213Version)) else job)
}
ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec(Zulu, "17"), JavaSpec(Zulu, "21"), JavaSpec(Zulu, "25"), JavaSpec(Zulu, "26"))
ThisBuild / githubWorkflowBuild := Seq(WorkflowStep.Sbt(List("test", "mimaReportBinaryIssues")))
ThisBuild / githubWorkflowTargetBranches := Seq("3.x", "3.2", "3.1", "3.0")
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
  // private to the introspect package: both gained the type a class captured by deriving ScalaTypeInfo
  ProblemFilters.exclude[Problem]("tools.jackson.module.scala.introspect.ClassHolder*"),
  ProblemFilters.exclude[Problem]("tools.jackson.module.scala.introspect.WrappedCreatorProperty*"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("tools.jackson.module.scala.ser.EitherSerializer.serialize"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("tools.jackson.module.scala.ser.TupleSerializer.this"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("tools.jackson.module.scala.ser.TypeTaggedSerializer.this"),
  ProblemFilters.exclude[MissingClassProblem]("tools.jackson.module.scala.ser.EnumSerializer$"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("tools.jackson.module.scala.ser.IterableSerializer.withResolved"),
  ProblemFilters.exclude[ReversedMissingMethodProblem]("tools.jackson.module.scala.ser.IterableSerializer.tools$jackson$module$scala$ser$IterableSerializer$*"),
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
  ProblemFilters.exclude[DirectMissingMethodProblem]("tools.jackson.module.scala.ser.IteratorSerializer.withResolved"),
  ProblemFilters.exclude[MissingClassProblem]("tools.jackson.module.scala.ser.EnumSerializerShared"),
  ProblemFilters.exclude[MissingClassProblem]("tools.jackson.module.scala.ser.EnumSerializerShared$"),
  // Scala 3 enum support keeps its cache on the module instance rather than in a singleton. Holding
  // that in a trait means a val, which Scala compiles to an accessor and a setter the implementing
  // class has to provide, so anyone extending these traits has to recompile.
  ProblemFilters.exclude[InheritedNewAbstractMethodProblem]("tools.jackson.module.scala.EnumModule.scala3EnumInfo"),
  ProblemFilters.exclude[InheritedNewAbstractMethodProblem]("tools.jackson.module.scala.EnumModule.tools$jackson$module$scala$Scala3EnumSupportState$_setter_$scala3EnumInfo_="),
  ProblemFilters.exclude[InheritedNewAbstractMethodProblem]("tools.jackson.module.scala.deser.EnumDeserializerModule.scala3EnumInfo"),
  ProblemFilters.exclude[InheritedNewAbstractMethodProblem]("tools.jackson.module.scala.deser.EnumDeserializerModule.tools$jackson$module$scala$Scala3EnumSupportState$_setter_$scala3EnumInfo_="),
  ProblemFilters.exclude[InheritedNewAbstractMethodProblem]("tools.jackson.module.scala.ser.EnumSerializerModule.scala3EnumInfo"),
  ProblemFilters.exclude[InheritedNewAbstractMethodProblem]("tools.jackson.module.scala.ser.EnumSerializerModule.tools$jackson$module$scala$Scala3EnumSupportState$_setter_$scala3EnumInfo_="),
  // What each class captured by deriving ScalaTypeInfo is remembered on the module instance rather
  // than in a singleton, for the same reason the enum cache above is. A trait that grows state grows
  // the accessors the implementing class has to provide, so anyone extending this trait has to
  // recompile - the field itself is private to the introspect package and cannot be called.
  ProblemFilters.exclude[ReversedMissingMethodProblem]("tools.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule._derivedTypeInfo"),
  ProblemFilters.exclude[ReversedMissingMethodProblem]("tools.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule._derivedTypeInfo_=")
)
