# CI Build

The release is done by GitHub Actions CI build. Snapshots are published after every successful (non-release) build.

## Release
* Update the `jacksonVersion` in the build.sbt. This controls the version of other Jackson dependencies that are used in the build.
* Commit this change and check that GitHub Actions CI build succeeds.
* One of the main reasons that it would fail is because jackson-module-scala tests with quite a few other Jackson libs and some of them may not yet be released to Maven Central. 
* If the build fails, try to rerun it using GitHub Actions menus. If necessary, make a new commit.
* If the build is ok, add a git tag of the form `v2.14.0` (note that the `v` prefix is required). Commit this tag.
* Check that build succeeds.

## Manual publishing

This is discouraged. If you must go this route, read the documentation for [sbt-sonatype](https://github.com/xerial/sbt-sonatype).

Use Java 8 when doing releases.
