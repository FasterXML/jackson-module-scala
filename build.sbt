Keys.`package` := {
  (Keys.`package` in (common, Compile)).value
  (Keys.`package` in (scala210, Compile)).value
  (Keys.`package` in (scala211, Compile)).value
}

lazy val root = project.in(file(".")).aggregate(common, scala211)

lazy val common = project

lazy val scala210 = project.in(file("scala-2.10"))
  .dependsOn(common)

lazy val scala211 = project.in(file("scala-2.11"))
  .dependsOn(common)

organization in ThisBuild := "com.fasterxml.jackson.module"

crossScalaVersions := Seq("2.10.4", "2.11.5")

scalacOptions in ThisBuild ++= Seq("-deprecation", "-unchecked", "-feature")

scalacOptions in (ThisBuild, Compile, compile) += "-Xfatal-warnings"

// Ensure jvm 1.6 for java
javacOptions in ThisBuild ++= Seq(
  "-source", "1.6",
  "-target", "1.6",
  "-bootclasspath", Seq(
    new File(System.getenv("JAVA6_HOME")) / "jre" / "lib" / "rt.jar",
    new File(System.getenv("JAVA6_HOME")) / ".." / "Classes" / "classes.jar"
  ).mkString(System.getProperty("path.separator"))
)

// Try to future-proof scala jvm targets, in case some future scala version makes 1.7 a default
scalacOptions in ThisBuild += "-target:jvm-1.6"
