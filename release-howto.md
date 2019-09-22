
For Scala 2.10 and 2.11 releases, use JDK7 to do the build and release.

For Scala 2.12 and 2.13 releases, use JDK7 to do the build and release.

Make sure the following file has the correct oss.sonatype.org-credentials:

	~/.ivy2/.credentials_sonatype

Perform the release (This will do everything, push to git, and promote artifacts in sonatype-system):

	sbt release
