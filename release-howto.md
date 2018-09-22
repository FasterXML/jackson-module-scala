

Set correct JAVA7_HOME on mac:

	export JAVA7_HOME=$(/usr/libexec/java_home -v 1.7)


Make sure the following file has the correct oss.sonatype.org-credentials:

	~/.ivy2/.credentials_sonatype

Perform the release (This will do everything, push to git, and promote artifacts in sonatype-system):

	sbt release
