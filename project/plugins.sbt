// For OSGI bundles
addSbtPlugin("com.typesafe.sbt" % "sbt-osgi" % "0.7.0")

// For making releases
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.3")

// For signing releases
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")

// For creating the github site
addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "0.8.1")

// For Eclipse support
addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "4.0.0")

resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"

addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.5.3")
