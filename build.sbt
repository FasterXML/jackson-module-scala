name := "jackson-module-scala"

organization := "com.fasterxml.jackson.module"

version := "2.2.0-SNAPSHOT"

resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"

crossScalaVersions := Seq("2.9.1", "2.9.2", "2.10.0-RC5")

libraryDependencies ++= Seq(
    "com.fasterxml.jackson.core" % "jackson-core" % "2.2.0-SNAPSHOT",
    "com.fasterxml.jackson.core" % "jackson-annotations" % "2.2.0-SNAPSHOT",
    "com.fasterxml.jackson.core" % "jackson-databind" % "2.2.0-SNAPSHOT",
    "com.fasterxml.jackson.module" %% "scalabeans" % "0.4-SNAPSHOT"
)

