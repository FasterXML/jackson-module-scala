import AssemblyKeys._
import PgpKeys._

// Basic facts
name := "jackson-module-scala"

organization := "com.fasterxml.jackson.module"

version := "2.1.3-SNAPSHOT"

crossScalaVersions := Seq("2.9.1", "2.9.2", "2.10.0")

libraryDependencies ++= Seq(
    // These are "provided" so that sbt-assembly doesn't include them (we'll fix the pom later)
    "com.fasterxml.jackson.core" % "jackson-core" % "2.1.2" % "provided",
    "com.fasterxml.jackson.core" % "jackson-annotations" % "2.1.2" % "provided",
    "com.fasterxml.jackson.core" % "jackson-databind" % "2.1.2" % "provided",
    // saying this is intransitive prevents the deps from being included in the jar
    "com.fasterxml.jackson.module" %% "scalabeans" % "0.4-SNAPSHOT" intransitive(),
    // but now we need to add scalabeans deps explicitly (also as "provided")
    "com.thoughtworks.paranamer" % "paranamer" % "2.3" % "provided",
    "com.google.guava" % "guava" % "13.0.1" % "provided",
    // test dependencies
    "org.scalatest" %% "scalatest" % "2.0.M5" % "test" cross CrossVersion.binaryMapped {
        case "2.10" => "2.10.0"
        case x => x
    },
    "junit" % "junit" % "4.11" % "test",
    "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % "2.1.2" % "test"
)

// Another scalabeans dependency, for 2.10 only
libraryDependencies <++= (scalaBinaryVersion, scalaVersion) { (sbv,sv) => sbv match {
  // again, "provided" so that it is not included in the jar
  case "2.10" => Seq("org.scala-lang" % "scala-reflect" % sv % "provided")
  case _ => Seq()
} }

// Change the classifier of the main jar
artifact in (Compile, packageBin) ~= { (art: Artifact) =>
  art.copy(`classifier` = Some("base"))
}

// Disable publishing the main jar
publishArtifact in (Compile, packageBin) := false

// setup sbt-assembly settings
assemblySettings

// don't assembly the scala dependency
assembleArtifact in packageScala := false

// don't add "assembly" to the jar name
jarName in assembly <<= (name, version, scalaBinaryVersion) { (name, version, sbv) =>
  name + "_" + sbv + "-" + version + ".jar"
}

// place the assembly in the versioned path
outputPath in assembly <<= (target in assembly, jarName in assembly, scalaBinaryVersion) { (t,s,v) =>
    t / ("scala-" + v) / s
}

// don't have a classifier on the assembly
artifact in (Compile, assembly) ~= { (art: Artifact) =>
  art.copy(`classifier` = None)
}

// add the assembly as an artifact
addArtifact(artifact in (Compile, assembly), assembly)

// add the assembly signature as an artifact
packagedArtifacts <<= (packagedArtifacts, pgpSigner, skip in pgpSigner, streams) map { (artifacts,r,skipZ,s) =>
  if (!skipZ) {
    artifacts flatMap {
      // This works because the assembly has already been declared with no classifer
      case (art @ Artifact(_,"jar","jar",None,_,_,_), file) =>
        Seq(art                                          -> file,
            art.copy(extension = art.extension + ".asc") -> r.sign(file, new File(file.getAbsolutePath + ".asc"), s))
      case (art, file) => Seq(art -> file)
    }
  } else artifacts
}

// pom re-writing:
//   * remove the scalabeans dependency, as it's included
//   * remove the 'provided' annotation on others, as they're not
pomPostProcess := {
    import xml.transform._
    new RuleTransformer(new RewriteRule {
        override def transform(node: xml.Node) = node match {
            case n if ((n \ "artifactId").text.startsWith("scalabeans")) => xml.NodeSeq.Empty
            case n @ <scope>{_*}</scope> if n.text == "provided" => xml.NodeSeq.Empty
            case _ => node
        }
    })
}

// publishing
publishMavenStyle := true

publishTo <<= version { v =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra := {
  <url>http://wiki.fasterxml.com/JacksonModuleScala</url>
  <licenses>
    <license>
      <name>The Apache Software License, Version 2.0</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <connection>scm:git:git@github.com:FasterXML/jackson-module-scala.git</connection>
    <developerConnection>scm:git:git@github.com:FasterXML/jackson-module-scala.git</developerConnection>
    <url>http://github.com/FasterXML/jackson-module-scala</url>
  </scm>
  <developers>
    <developer>
      <id>tatu</id>
      <name>Tatu Saloranta</name>
      <email>tatu@fasterxml.com</email>
    </developer>
    <developer>
      <id>christopher</id>
      <name>Christopher Currie</name>
      <email>christopher@currie.com</email>
    </developer>
  </developers>
  <contributors>
    <contributor>
      <name>Nathaniel Bauernfeind</name>
    </contributor>
    <contributor>
      <name>Anton Panasenko</name>
    </contributor>
  </contributors>
}

// credentials
credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

// release
releaseSettings
