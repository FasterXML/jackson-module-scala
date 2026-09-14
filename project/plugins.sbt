// For OSGI bundles
addSbtPlugin("com.github.sbt" % "sbt-osgi" % "0.10.0")

// For creating the github site
addSbtPlugin("com.github.sbt" % "sbt-site" % "1.8.0")

//addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.6.3")

addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "1.2.0")

addSbtPlugin("com.github.sbt" % "sbt-github-actions" % "0.32.0")

addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.3.2")
addSbtPlugin("com.github.sbt" % "sbt-git" % "2.2.0")
addSbtPlugin("com.github.sbt" % "sbt-dynver" % "5.1.1")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.12.2")
addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.12.1")

addSbtPlugin("net.bzzt" % "sbt-reproducible-builds" % "0.35")

addSbtPlugin("com.github.sbt" %% "sbt-sbom" % "0.5.0")
