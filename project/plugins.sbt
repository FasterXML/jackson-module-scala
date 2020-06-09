// For OSGI bundles
addSbtPlugin("com.typesafe.sbt" % "sbt-osgi" % "0.9.5")

// For making releases
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.12")

// For signing releases
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "2.0.0")

// For creating the github site
addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "1.4.0")

// For Eclipse support
addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "5.2.4")

addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.6.3")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.8")
