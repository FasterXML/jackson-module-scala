// For OSGI bundles
addSbtPlugin("com.typesafe.sbt" % "sbt-osgi" % "0.9.6")

// For making releases
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.13")

// For signing releases
addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.1.2")

// For creating the github site
addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "1.4.0")

// For Eclipse support
addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "5.2.4")

addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.6.3")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.8")

addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.8.1")

addSbtPlugin("ch.epfl.lamp" % "sbt-dotty" % "0.5.4")
