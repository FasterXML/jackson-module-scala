// For shading the jar
// addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.9.0")
addSbtPlugin("com.typesafe.sbt" % "sbt-proguard" % "0.2.1")

// For making releases
addSbtPlugin("com.github.gseitz" % "sbt-release" % "0.7")

// For signing releases
addSbtPlugin("com.typesafe.sbt" % "sbt-pgp" % "0.8")
