[![Build Status](https://travis-ci.com/FasterXML/jackson-module-scala.svg?branch=2.12)](https://travis-ci.com/FasterXML/jackson-module-scala) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.fasterxml.jackson.module/jackson-module-scala_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.fasterxml.jackson.module/jackson-module-scala_2.12)

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

# Usage

To use the Scala Module in Jackson, simply register it with the
ObjectMapper instance:

```scala
// With 2.10 and later
val mapper = JsonMapper.builder()
  .addModule(DefaultScalaModule)
  .build()

// versions before 2.10 (also support for later 2.x but not 3.0)
val mapper = new ObjectMapper()
mapper.registerModule(DefaultScalaModule)
```

`DefaultScalaModule` is a Scala object that includes support for all
currently supported Scala data types. If only partial support is desired,
the component traits can be included individually:

```scala
val module = new OptionModule with TupleModule {}
val mapper = JsonMapper.builder()
  .addModule(module)
  .build()
```

You can also mixin `ScalaObjectMapper` to get rich wrappers that automatically
convert scala manifests directly into TypeReferences for Jackson to use (now depecated because manifests are not supported in Scala 3):
```scala
val mapper = new ObjectMapper() with ScalaObjectMapper
mapper.registerModule(DefaultScalaModule)
val myMap = mapper.readValue[Map[String,Tuple2[Int,Int]]](src)
```

This is the equivalent of
```scala
val mapper = JsonMapper.builder().addModule(DefaultScalaModule).build()
val myMap = mapper.readValue(src, new TypeReference[Map[String,Tuple2[Int,Int]]]{})
```

Consult the [Scaladoc](http://fasterxml.github.io/jackson-module-scala/latest/api/) for further details.

# Building

The master branch often depends on SNAPSHOT versions of the core Jackson projects,
which are published to the Sonatype OSS Repository. To make these dependencies available,
create a file called `sonatype.sbt` in the same directory as `build.sbt` with the following
content. The project `.gitignore` file intentionally prevents this file from being checked in.

``` scala
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
```

# Download, docs

Check out [Wiki]. API Scaladocs can be found [on the project site][API] but they are not really
well suited to end users, as most classes are implementation details of the module.

# Contributing

The main mechanisms for contribution are:

* Reporting issues, suggesting improved functionality on Github issue tracker
* Participating in discussions on mailing lists, Gitter (see [Jackson portal](https://github.com/FasterXML/jackson#participation) for details)
* Submitting Pull Requests (PRs) to fix issues, improve functionality.

## Core Development Team

Currently active core developers (ones who can review, accept and merge Pull Requests) are:

* Morten Kjetland (@mbknor)
* Nate Bauernfeind (@nbauernfeind)
* PJ Fanning (@pjfanning)

If you have questions on issues, implementation strategies, you may refer to core developers
(and this is recommended if you are in doubt!), but keep in mind that these are voluntary
positions: everyone is doing this because they want to, not because they are paid or
contractually obligated to. This also means that time availability changes over time
so getting answers may take time.

In addition other Jackson developers with similar access (but less active) include

* Christopher Currie (@christophercurrie) -- original author of Scala module
* Tatu Saloranta (@cowtowncoder) -- main author of core Jackson components

# Acknowledgements

[![Developed with IntelliJ IDEA](http://www.jetbrains.com/img/logos/logo_intellij_idea.png "Developed with IntelliJ IDEA")](http://www.jetbrains.com/idea/features/scala.html)

[Jackson]: https://github.com/FasterXML/jackson
[SAX]: http://www.saxproject.org/
[DOM]: http://www.w3.org/TR/DOM-Level-3-Core/
[JAXB]: http://jaxb.java.net/
[Jersey]: http://jersey.java.net/
[Java Bean]: http://www.oracle.com/technetwork/java/javase/documentation/spec-136004.html
[Scala]: http://www.scala-lang.org/
[Wiki]: https://github.com/FasterXML/jackson-module-scala/wiki
[API]: http://fasterxml.github.io/jackson-module-scala/latest/api/#com.fasterxml.jackson.module.scala.package
