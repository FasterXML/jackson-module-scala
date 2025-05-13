![Build Status](https://github.com/FasterXML/jackson-module-scala/actions/workflows/ci.yml/badge.svg?branch=3.x)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/tools.jackson.module/jackson-module-scala_2.13/badge.svg)](https://maven-badges.herokuapp.com/maven-central/tools.jackson.module/jackson-module-scala_2.13)
[![Sonatype Snapshots](https://img.shields.io/nexus/s/https/s01.oss.sonatype.org/tools.jackson.module/jackson-module-scala_2.13.svg)](https://s01.oss.sonatype.org/content/repositories/snapshots/tools/jackson/module/jackson-module-scala_2.13/)
[![Tidelift](https://tidelift.com/badges/package/maven/com.fasterxml.jackson.module:jackson-module-scala_2.13)](https://tidelift.com/subscription/pkg/maven-com.fasterxml.jackson.module.jackson-module-scala.2.13?utm_source=maven-com.fasterxml.jackson.module.jackson-module-scala.2.13&utm_medium=github_sponsor_button&utm_campaign=readme)

# Overview

[Jackson] is a fast JSON processor for Java that supports three models:
streaming, node, and object mapping (akin to the three independent models
[SAX]/[Stax], [DOM] and [JAXB] for XML processing).

The object mapping model is a high-level processing model that allows the
user to project JSON data onto a domain-specific data model appropriate
for their application, without having to deal with the low-level mechanics
of JSON parsing. It is the standard object mapping parser implementaton
in [Jersey], the reference implementation for JSR-311
(Java API for
Restful Web Services).

[Scala] is a functional programming language for the JVM that supports
Java interoperability. Its standard library is quite distinct from Java,
and does not fulfill the expectations of Jacksons default mappings.
Notably, Scala collections do not derive from `java.util.Collection` or
its subclasses, and Scala properties do not (by default) look like `Java Bean` properties.

The Scala Module supports serialization and limited deserialization of
Scala Case Classes, `Sequence`s, `Map`s, `Tuple`s, `Option`s, and Enumerations.

# Version Support

Jackson-module-scala follows the same release strategy of [jackson-databind](https://github.com/FasterXML/jackson-databind).
3.x branch is used for Jackson 3 development.

Scala 2.12, 2.13, 3.3+ are supported. Scala 2.11 support was dropped in v3.0.0. Java 17 is
the minimum supported version now (Jackson 3 generally has a minimum requirement of Java 17).

## Scala 3

There are a few differences from Scala 2 support.
* There are still a few tests that work with Scala 2 that fail with Scala 3
* It is expected that most use cases should work ok with Scala 3
  * Known issues with using jackson-module-scala with Scala 3 are tracked at https://github.com/FasterXML/jackson-module-scala/labels/scala3
  * There has been limited testing of using Scala 3 classes with Scala 2 jackson-module-scala or Scala 2 classes with Scala 3 jackson-module-scala

# Usage

To use the Scala Module in Jackson, simply register it with the
ObjectMapper instance:

```scala
val mapper = JsonMapper.builder()
  .addModule(DefaultScalaModule)
  .build()
```

`DefaultScalaModule` is a Scala object that includes support for all
currently supported Scala data types. If only partial support is desired,
the component traits can be included individually (approach differs from Jackson 2):

```scala
val scalaModule = ScalaModule.builder()
  .addModule(OptionModule)
  .addModule(TupleModule)
  .build()

val mapper = JsonMapper.builder()
  .addModule(scalaModule)
  .build()
```
If you want to configure the behavior of the ScalaModule but have all the underlying Scala modules, you can do this :

```scala
val scalaModule = ScalaModule.builder()
  .addAllBuiltinModules()
  .addModule(TupleModule)
  .build()

val mapper = JsonMapper.builder()
  .addModule(scalaModule)
  .applyDefaultValuesWhenDeserializing(false) //default of true
  .supportScala3Classes(false) //default of true
  .build()
```

## ClassTagExtensions
You can also mixin `ClassTagExtensions` to get rich wrappers that automatically
convert scala ClassTags directly into TypeReferences for Jackson to use:
```scala
val mapper = JsonMapper.builder().addModule(DefaultScalaModule).build() :: ClassTagExtensions
val myMap = mapper.readValue[Map[String, Tuple2[Int,Int]]](src)
```

ClassTagExtensions is a replacement for `ScalaObjectMapper`, which was recently deprecated because it relies on `Manifest`s and they are not supported in Scala 3.

This is the equivalent of
```scala
val mapper = JsonMapper.builder().addModule(DefaultScalaModule).build()
val myMap = mapper.readValue(src, new TypeReference[Map[String,Tuple2[Int,Int]]]{})
```

Consult the [Scaladoc](https://fasterxml.github.io/jackson-module-scala/latest/api/) for further details.

## Sbt

To import in sbt:
```scala
libraryDependencies += "tools.jackson.module" %% "jackson-module-scala" % "3.0.0-rc1-SNAPSHOT"
```

## Java/Kotlin users

DefaultScalaModule is a Scala Object and to access it when you are not compiling with Scala compiler, you will need to use `tools.jackson.module.scala.javadsl.DefaultScalaModule.getInstance()` instead. You can access the Scala object using `tools.jackson.module.scala.DefaultScalaModule$.MODULE$`.

```java
import tools.jackson.module.scala.javadsl.*;

ObjectMapper mapper = JsonMapper.builder().addModule(DefaultScalaModule.getInstance()).build();
// or ScalaModule.builder().addAllBuiltinModules().build() instead of DefaultScalaModule.getInstance()
```

# Building

The branches often depends on SNAPSHOT versions of the core Jackson projects,
which are published to the Sonatype OSS Repository. To make these dependencies available,
create a file called `sonatype.sbt` in the same directory as `build.sbt` with the following
content. The project `.gitignore` file intentionally prevents this file from being checked in.

``` scala
resolvers ++= Resolver.sonatypeOssRepos("snapshots")
```

# Download, docs

Check out [Wiki]. API Scaladocs can be found [on the project site][API] but they are not really
well suited to end users, as most classes are implementation details of the module.

# Related Projects
* [jackson-scala-reflect-extensions](https://github.com/pjfanning/jackson-scala-reflect-extensions)
* [jackson-scala3-reflect-extensions](https://github.com/pjfanning/jackson-scala3-reflection-extensions)
* [jackson-module-enumeratum](https://github.com/pjfanning/jackson-module-enumeratum)
* [jackson-caffeine-cache](https://github.com/pjfanning/jackson-caffeine-cache)

# Contributing

The main mechanisms for contribution are:

* Reporting issues, suggesting improved functionality on Github issue tracker
* Participating in discussions on mailing lists, Gitter (see [Jackson portal](https://github.com/FasterXML/jackson#participation) for details)
* Submitting Pull Requests (PRs) to fix issues, improve functionality.

## Support

### Community support

Jackson components are supported by the Jackson community through mailing lists, Gitter forum, Github issues. See [Participation, Contributing](../../../jackson#participation-contributing) for full details.


### Enterprise support

Available as part of the Tidelift Subscription.

The maintainers of `jackson-module-scala` and thousands of other packages are working with Tidelift to deliver commercial support and maintenance for the open source dependencies you use to build your applications. Save time, reduce risk, and improve code health, while paying the maintainers of the exact dependencies you use. [Learn more.](https://tidelift.com/subscription/pkg/maven-com.fasterxml.jackson.module.jackson-module-scala.2.13?utm_source=maven-com.fasterxml.jackson.module.jackson-module-scala.2.13&utm_medium=referral&utm_campaign=enterprise&utm_term=repo)

## Core Development Team

Currently active core developers (ones who can review, accept and merge Pull Requests) are:

* PJ Fanning (@pjfanning)

If you have questions on issues, implementation strategies, you may refer to core developers
(and this is recommended if you are in doubt!), but keep in mind that these are voluntary
positions: everyone is doing this because they want to, not because they are paid or
contractually obligated to. This also means that time availability changes over time
so getting answers may take time.

In addition, other Jackson developers with similar access (but less active) include:

* Christopher Currie (@christophercurrie) -- original author of Scala module
* Morten Kjetland (@mbknor)
* Nate Bauernfeind (@nbauernfeind)
* Tatu Saloranta (@cowtowncoder) -- main author of core Jackson components

# Acknowledgements

[![Developed with IntelliJ IDEA](https://www.jetbrains.com/img/logos/logo_intellij_idea.png "Developed with IntelliJ IDEA")](https://www.jetbrains.com/idea/features/scala.html)

[Jackson]: https://github.com/FasterXML/jackson
[SAX]: https://www.saxproject.org/
[DOM]: https://www.w3.org/TR/DOM-Level-3-Core/
[JAXB]: https://jaxb.java.net/
[Jersey]: https://jersey.java.net/
[Java Bean]: https://www.oracle.com/technetwork/java/javase/documentation/spec-136004.html
[Scala]: https://www.scala-lang.org/
[Wiki]: https://github.com/FasterXML/jackson-module-scala/wiki
[API]: https://fasterxml.github.io/jackson-module-scala/latest/api/#tools.jackson.module.scala.package
