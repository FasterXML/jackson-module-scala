import com.github.sbt.sbom._
import com.typesafe.tools.mima.core._
import sbtghactions.JavaSpec.Distribution.Zulu

// Basic facts
name := "jackson-module-scala"

organization := "com.fasterxml.jackson.module"

val scala213Version = "2.13.16"
ThisBuild / scalaVersion := scala213Version

ThisBuild / crossScalaVersions := Seq("2.11.12", "2.12.20", scala213Version, "3.3.6")

resolvers ++= Resolver.sonatypeOssRepos("snapshots")

publishTo := sonatypePublishToBundle.value

bomFormat := "xml"

version := "2.20.0-SNAPSHOT"
val jacksonCoreVersion = "2.20.0-SNAPSHOT"
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
    "com.fasterxml.jackson.core" % "jackson-annotations" % jacksonCoreVersion changing(),
    "com.fasterxml.jackson.core" % "jackson-databind" % jacksonCoreVersion changing()
  )
else
  Seq(
    "com.fasterxml.jackson.core" % "jackson-core" % jacksonCoreVersion,
    "com.fasterxml.jackson.core" % "jackson-annotations" % jacksonCoreVersion,
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
      "SONATYPE_PASSWORD" -> "${{ secrets.CI_DEPLOY_PASSWORD }}",
      "SONATYPE_USERNAME" -> "${{ secrets.CI_DEPLOY_USERNAME }}",
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
  ProblemFilters.exclude[ReversedMissingMethodProblem]("com.fasterxml.jackson.module.scala.util.ClassW.isScalaObject"),
  ProblemFilters.exclude[ReversedMissingMethodProblem]("com.fasterxml.jackson.module.scala.util.ClassW.extendsScalaClass"),
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
  ProblemFilters.exclude[ReversedMissingMethodProblem]("com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule.com$fasterxml$jackson$module$scala$introspect$ScalaAnnotationIntrospectorModule$$_shouldSupportScala3Classes_="),
  ProblemFilters.exclude[ReversedMissingMethodProblem]("com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule.com$fasterxml$jackson$module$scala$introspect$ScalaAnnotationIntrospectorModule$$_shouldSupportScala3Classes"),
  ProblemFilters.exclude[ReversedMissingMethodProblem]("com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule.com$fasterxml$jackson$module$scala$introspect$ScalaAnnotationIntrospectorModule$$_lookupCacheFactory"),
  ProblemFilters.exclude[ReversedMissingMethodProblem]("com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule.com$fasterxml$jackson$module$scala$introspect$ScalaAnnotationIntrospectorModule$$_lookupCacheFactory_="),
  ProblemFilters.exclude[ReversedMissingMethodProblem]("com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule.com$fasterxml$jackson$module$scala$introspect$ScalaAnnotationIntrospectorModule$$_descriptorCacheSize"),
  ProblemFilters.exclude[ReversedMissingMethodProblem]("com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule.com$fasterxml$jackson$module$scala$introspect$ScalaAnnotationIntrospectorModule$$_descriptorCacheSize_="),
  ProblemFilters.exclude[ReversedMissingMethodProblem]("com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule.com$fasterxml$jackson$module$scala$introspect$ScalaAnnotationIntrospectorModule$$_scalaTypeCacheSize"),
  ProblemFilters.exclude[ReversedMissingMethodProblem]("com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule.com$fasterxml$jackson$module$scala$introspect$ScalaAnnotationIntrospectorModule$$_scalaTypeCacheSize_="),
  ProblemFilters.exclude[ReversedMissingMethodProblem]("com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule.setLookupCacheFactory"),
  ProblemFilters.exclude[ReversedMissingMethodProblem]("com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule.setDescriptorCacheSize"),
  ProblemFilters.exclude[ReversedMissingMethodProblem]("com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule.setScalaTypeCacheSize"),
  ProblemFilters.exclude[ReversedMissingMethodProblem]("com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule.clearRegisteredReferencedTypes"),
  ProblemFilters.exclude[ReversedMissingMethodProblem]("com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule.registerReferencedValueType"),
  ProblemFilters.exclude[ReversedMissingMethodProblem]("com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule.getRegisteredReferencedValueType"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospector#ScalaValueInstantiator.this"),
  ProblemFilters.exclude[ReversedMissingMethodProblem]("com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule._scalaTypeCache"),
  ProblemFilters.exclude[ReversedMissingMethodProblem]("com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule._scalaTypeCache_="),
  ProblemFilters.exclude[ReversedMissingMethodProblem]("com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule.setScalaTypeCache"),
  ProblemFilters.exclude[ReversedMissingMethodProblem]("com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule.shouldSupportScala3Classes"),
  ProblemFilters.exclude[ReversedMissingMethodProblem]("com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule.supportScala3Classes"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.fasterxml.jackson.module.scala.deser.NumberDeserializers.BigIntClass"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.fasterxml.jackson.module.scala.deser.NumberDeserializers.BigDecimalClass"),
  ProblemFilters.exclude[IncompatibleResultTypeProblem]("com.fasterxml.jackson.module.scala.deser.BigDecimalDeserializer.deserialize"),
  ProblemFilters.exclude[IncompatibleResultTypeProblem]("com.fasterxml.jackson.module.scala.deser.BigIntDeserializer.deserialize"),
  ProblemFilters.exclude[MissingTypesProblem]("com.fasterxml.jackson.module.scala.deser.BigDecimalDeserializer$"),
  ProblemFilters.exclude[MissingTypesProblem]("com.fasterxml.jackson.module.scala.deser.BigIntDeserializer$"),
  ProblemFilters.exclude[MissingClassProblem]("com.fasterxml.jackson.module.scala.deser.BigNumberDeserializer"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.fasterxml.jackson.module.scala.deser.GenericMapFactoryDeserializerResolver#BuilderWrapper.this"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.fasterxml.jackson.module.scala.DefaultRequiredAnnotationIntrospector.findDeserializationContentType"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.fasterxml.jackson.module.scala.DefaultRequiredAnnotationIntrospector.findDeserializationKeyType"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.fasterxml.jackson.module.scala.DefaultRequiredAnnotationIntrospector.findDeserializationType"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.fasterxml.jackson.module.scala.DefaultRequiredAnnotationIntrospector.findSerializationContentType"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.fasterxml.jackson.module.scala.DefaultRequiredAnnotationIntrospector.findSerializationKeyType"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.fasterxml.jackson.module.scala.DefaultRequiredAnnotationIntrospector.findSerializationType"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.fasterxml.jackson.module.scala.DefaultRequiredAnnotationIntrospector.findSerializationInclusionForContent"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.fasterxml.jackson.module.scala.DefaultRequiredAnnotationIntrospector.findSerializationInclusion"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.fasterxml.jackson.module.scala.experimental.DefaultRequiredAnnotationIntrospector.findDeserializationContentType"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.fasterxml.jackson.module.scala.experimental.DefaultRequiredAnnotationIntrospector.findDeserializationKeyType"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.fasterxml.jackson.module.scala.experimental.DefaultRequiredAnnotationIntrospector.findDeserializationType"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.fasterxml.jackson.module.scala.experimental.DefaultRequiredAnnotationIntrospector.findSerializationContentType"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.fasterxml.jackson.module.scala.experimental.DefaultRequiredAnnotationIntrospector.findSerializationKeyType"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.fasterxml.jackson.module.scala.experimental.DefaultRequiredAnnotationIntrospector.findSerializationType"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.fasterxml.jackson.module.scala.experimental.DefaultRequiredAnnotationIntrospector.findSerializationInclusionForContent"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.fasterxml.jackson.module.scala.experimental.DefaultRequiredAnnotationIntrospector.findSerializationInclusion"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.fasterxml.jackson.module.scala.introspect.JavaAnnotationIntrospector.findDeserializationContentType"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.fasterxml.jackson.module.scala.introspect.JavaAnnotationIntrospector.findDeserializationKeyType"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.fasterxml.jackson.module.scala.introspect.JavaAnnotationIntrospector.findDeserializationType"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.fasterxml.jackson.module.scala.introspect.JavaAnnotationIntrospector.findSerializationContentType"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.fasterxml.jackson.module.scala.introspect.JavaAnnotationIntrospector.findSerializationKeyType"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.fasterxml.jackson.module.scala.introspect.JavaAnnotationIntrospector.findSerializationType"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.fasterxml.jackson.module.scala.introspect.JavaAnnotationIntrospector.findSerializationInclusionForContent"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.fasterxml.jackson.module.scala.introspect.JavaAnnotationIntrospector.findSerializationInclusion"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospector.findDeserializationContentType"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospector.findDeserializationKeyType"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospector.findDeserializationType"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospector.findSerializationContentType"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospector.findSerializationKeyType"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospector.findSerializationType"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospector.findSerializationInclusionForContent"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospector.findSerializationInclusion"),
  ProblemFilters.exclude[ReversedMissingMethodProblem]("com.fasterxml.jackson.module.scala.util.ClassW.getModuleField"),
  ProblemFilters.exclude[ReversedMissingMethodProblem]("com.fasterxml.jackson.module.scala.util.ClassW.com$fasterxml$jackson$module$scala$util$ClassW$$moduleField"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.fasterxml.jackson.module.scala.DefaultRequiredAnnotationIntrospector.findIgnoreUnknownProperties"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.fasterxml.jackson.module.scala.DefaultRequiredAnnotationIntrospector.findPropertiesToIgnore"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.fasterxml.jackson.module.scala.experimental.DefaultRequiredAnnotationIntrospector.findIgnoreUnknownProperties"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.fasterxml.jackson.module.scala.experimental.DefaultRequiredAnnotationIntrospector.findPropertiesToIgnore"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.fasterxml.jackson.module.scala.introspect.JavaAnnotationIntrospector.findIgnoreUnknownProperties"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.fasterxml.jackson.module.scala.introspect.JavaAnnotationIntrospector.findPropertiesToIgnore"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospector.findIgnoreUnknownProperties"),
  ProblemFilters.exclude[DirectMissingMethodProblem]("com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospector.findPropertiesToIgnore")
)

