Check VERSION.md for jackson-module-scala 2.x release information. This file tracks jackson-module-scala 3.x.

# 3.1.2

There are no new fixes in this release; it serves to track the 3.1.2 release
of the upstream Jackson projects.

# 3.1.1

There are no new fixes in this release; it serves to track the 3.1.1 release
of the upstream Jackson projects.

# 3.1.0

This patch release tracks Jackson 3.1.0. The 3.1 series has full support for Scala 2.12, 2.13 and 3.

* [[gh-787]](https://github.com/FasterXML/jackson-module-scala/issues/787) support JsonInclude filter for Scala collections
* [[gh-pr-799]](https://github.com/FasterXML/jackson-module-scala/pull/799) Scala 3.8+ requires us to make an underlying field in a Scala 2 Enumeration class accessible

# 3.0.4

There are no new fixes in this release; it serves to track the 3.0.4 release
of the upstream Jackson projects.

# 3.0.3

## Fixes

* [[gh-781]](https://github.com/FasterXML/jackson-module-scala/issues/781) Cannot deserialize Scala case class when JsonProperty annotation is used with var constructor parameter

# 3.0.2

## Fixes

* [[gh-pr-773]](https://github.com/FasterXML/jackson-module-scala/pull/773) fix serialization issue with alphabetic sorting of properties

# 3.0.1

There are no new fixes in this release; it serves to track the 3.0.1 release
of the upstream Jackson projects.

# 3.0.0

This patch release tracks Jackson 3.0.0. The 3.0 series has full support for Scala 2.12, 2.13 and 3.
There are many changes in related Jackson libraries that affect all Jackson users. jackson-module-scala changes
are less significant. The most notable change is the addition of ScalaModule builder.
Users who don't use DefaultScalaModule may need to change to using ScalaModule builder.

* [[gh-525]](https://github.com/FasterXML/jackson-module-scala/issues/525) Add ScalaModule builder
* [[gh-531]](https://github.com/FasterXML/jackson-module-scala/issues/531) Remove DefaultRequiredAnnotationIntrospector as part of dropping support for jackson-module-jsonSchema
* [[gh-583]](https://github.com/FasterXML/jackson-module-scala/issues/583) Change groupId and package names to `tools.jackson`
* [[gh-pr-665]](https://github.com/FasterXML/jackson-module-scala/pull/665) Remove ScalaObjectMapper in favour of ClassTagExtensions
* [[gh-pr-707]](https://github.com/FasterXML/jackson-module-scala/pull/707) Drop Scala 2.11 support and remove dependency on paranamer
* [[gh-pr-720]](https://github.com/FasterXML/jackson-module-scala/pull/720) When deserializing Scala collections and the input is missing or null, create an empty collection.
* [[gh-pr-759]](https://github.com/FasterXML/jackson-module-scala/pull/759) Remove ClassTagExtensions support for URL inputs
