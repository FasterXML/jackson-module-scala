

Set correct JAVA7_HOME on mac:

	export JAVA7_HOME=$(/usr/libexec/java_home -v 1.7)


Make sure the following file has the correct oss.sonatype.org-credentials:

	~/.ivy2/.credentials_sonatype

Perform the release:

	sbt release

Log into oss.sonatype.org and inspect the artifacts in staging, then close and release them


push local changes with tags

	git push
	git push --tags

