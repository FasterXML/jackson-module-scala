# Version 2.2.0

Version notes for this and future versions can be found in the `notes`
subdirectory.

# Version: 2.1.3

This release is the first to support multiple Scala versions. For Maven
users, note that the artifact id has changed to include the Scala version.
Scala 2.9.1, 2.9.2, and 2.10.0 are currently supported.

## Fixes

* [[gh-51]](https://github.com/FasterXML/jackson-module-scala/issues/51)
  Pick up JsonTypeInfo on class parameters, from Rintcius Blok

# Version: 2.1.2

## Fixes

* [[gh-48]](https://github.com/FasterXML/jackson-module-scala/issues/48)
  Serialization for byte arrays. This was due to an outdated transitive
  dependency that has been updated.

# Version: 2.1.1, 2.0.4

## Fixes

* [[gh-32]](https://github.com/FasterXML/jackson-module-scala/issues/32)
  [[gh-45]](https://github.com/FasterXML/jackson-module-scala/issues/45)
  `NON_NULL` for case classes. This was erroneously reported as having been
  fixed in 2.0.3, because of an incorrect focus on `Option`.

# Version: 2.1.0

This release is a new minor version release to go along with the Jackson 2.1
series, and fixes some minor incompatibilies with that release. It is
functionally identical to version 2.0.3.

# Version: 2.0.3

This release is a bugfix release for the Jackson 2.x series. It addresses the
following issues:

## Fixes

* [[gh-32]](https://github.com/FasterXML/jackson-module-scala/issues/32)
  `NON_NULL` for Option

* [[gh-41]](https://github.com/FasterXML/jackson-module-scala/issues/40)
  Symbol property name support, from Ivan Porto Carrero

* [[gh-43]](https://github.com/FasterXML/jackson-module-scala/pull/43)
  Scala Iterator support, from Heikki Vesalainen

* [[gh-38]](https://github.com/FasterXML/jackson-module-scala/issues/38)
  Honor property naming stratey, from Bill Crook

* [[gh-36]](https://github.com/FasterXML/jackson-module-scala/issues/36)
  Deserialization issue with Tuple in containers

* [[gh-26]](https://github.com/FasterXML/jackson-module-scala/issues/26)
  ScalaBeans memory leak

* [[gh-9]](https://github.com/FasterXML/jackson-module-scala/issues/9)
  [[gh-23]](https://github.com/FasterXML/jackson-module-scala/issues/32)
  Support the `JsonInclude.Include.NON_EMPTY` annotation parameter

# Version: 2.0.2

This release is a bugfix release for the Jackson 2.x series:
no changes to this module, but updated to depend on 2.0.2 versions
of core components which have fixes.

## Miscellaneous

* Updated the release notes, which were out of date in the 2.0.0 release.

# Version: 2.0.0

This release is the first release for the Jackson 2.x series. There is one
new fix for this release, otherwise it is functionally equivalent to
the 1.9 series.

## Fixes

* [[gh-25]](https://github.com/FasterXML/jackson-module-scala/pull/25):
  Support parameterized case classes, from Nathaniel Bauernfeind

# Version: 1.9.3

Release Date: 07-Mar-2012

This release will be the last feature release for 1.9.x. Bugfixes from 2.0 may be
backported on a case-by-case basis.

## Fixes

* [[gh-12](https://github.com/FasterXML/jackson-module-scala/issues/12)]:
  Added UntypedObjectDeserializer


### Improvements

* [[gh-17](https://github.com/FasterXML/jackson-module-scala/pull/17)]:
  New test case for Options nested inside of Case Class, from Nathaniel Bauernfeind

* [[gh-18](https://github.com/FasterXML/jackson-module-scala/pull/18)]:
  Support @JsonIgnoreProperty annotations, from Anton Panasenko

* [[gh-22](https://github.com/FasterXML/jackson-module-scala/pull/2)]:
  Unsorted Set deserialization support, from Nathaniel Bauernfeind
