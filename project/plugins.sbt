// For OSGI bundles
addSbtPlugin("com.typesafe.sbt" % "sbt-osgi" % "0.7.0")

// For making releases
addSbtPlugin("com.github.gseitz" % "sbt-release" % "0.8.5")

// For signing releases
addSbtPlugin("com.typesafe.sbt" % "sbt-pgp" % "0.8.2")

// For creating the github site
addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "0.8.1")

resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"

addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.5.3")
