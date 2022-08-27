# CI Build

The release is done by Github Actions CI build. Add a git tag of the form `v2.14.0` (note that the `v` prefix is required).
Snapshots are published after every successful build.

## Manual publishing

This is discouraged. If you must go this route, read the documentation for [sbt-sonatype](https://github.com/xerial/sbt-sonatype).
