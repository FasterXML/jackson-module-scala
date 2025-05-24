// For OSGI bundles
addSbtPlugin("com.typesafe.sbt" % "sbt-osgi" % "0.9.6")

// For creating the github site
addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "1.4.0")

//addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.6.3")

addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "1.1.4")

addSbtPlugin("com.github.sbt" % "sbt-github-actions" % "0.25.0")

addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.3.1")
addSbtPlugin("com.github.sbt" % "sbt-git" % "2.1.0")
addSbtPlugin("com.github.sbt" % "sbt-dynver" % "5.1.0")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.12.2")
addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.11.0")

addSbtPlugin("net.bzzt" % "sbt-reproducible-builds" % "0.32")

addSbtPlugin("com.github.sbt" %% "sbt-sbom" % "0.4.0")
