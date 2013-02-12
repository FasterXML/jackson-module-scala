# Overview

[Jackson][] is a fast JSON processor for Java that supports three models:
streaming, node, and object mapping (akin to the three independent models
[SAX][], [DOM][], and [JAXB][] in XML).

The object mapping model is a high-level processing model that allows the
user to project JSON data onto a domain-specific data model appropriate
for their application, without having to deal with the low-level mechanics
of JSON parsing. It is the standard object mapping parser implementaton
in [Jersey][], the reference implementation for JSR-311 (Java API for
Restful Web Services).

[Scala][] is a functional programming language for the JVM that supports
Java interoperability. Its standard library is quite distinct from Java,
and does not fulfill the expectations of Jacksons default mappings.
Notably, Scala collections do not derive from `java.util.Collection` or
its subclasses, and Scala properties do not (by default) look like [Java
Bean][] properties.

The Scala Module supports serialization and limited deserialization of
Scala Case Classes, Sequences, Maps, Tuples, Options, and Enumerations.

## Caveats

Support for class constructor arguments currently depends upon [Paranamer]
(http://paranamer.codehaus.org/), specifically an implementation that
depends upon constructor parameter names being present in the class debug
information. Since this is the default in Scala, it is usually not an
issue, but since it's possible to turn this off, be aware that the current
version will throw an exception if it cannot find the constructor parameter
names. Future versions may permit configuration to suppress this exception.

[![Build Status](https://fasterxml.ci.cloudbees.com/job/jackson-module-scala-master-sbt/badge/icon)](https://fasterxml.ci.cloudbees.com/job/jackson-module-scala-master-sbt/)

# Usage

To use the Scala Module in Jackson, simply register it with the
ObjectMapper instance:

```scala
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)
```

`DefaultScalaModule` is a Scala object that includes support for all
currently supported Scala data types. If only partial support is desired,
the component traits can be included individually:

```scala
    val module = new OptionModule with TupleModule {}
    val mapper = new ObjectMapper()
    mapper.registerModule(module)
```

You can also mixin `ScalaObjectMapper` (experimental) to get rich wrappers that automatically
convert scala manifests directly into TypeReferences for Jackson to use:
```scala
    val mapper = new ObjectMapper() with ScalaObjectMapper
    mapper.registerModule(DefaultScalaModule)
    val myMap = mapper.readValue[Map[String,Tuple2[Int,Int]]](src)
```

Consult the Scaladoc for further details.

# Download, docs

Check out [Wiki].

[Jackson]: http://jackson.codehaus.org/
[SAX]: http://www.saxproject.org/
[DOM]: http://www.w3.org/TR/DOM-Level-3-Core/
[JAXB]: http://jaxb.java.net/
[Jersey]: http://jersey.java.net/
[Java Bean]: http://www.oracle.com/technetwork/java/javase/documentation/spec-136004.html
[Scala]: http://www.scala-lang.org/
[Wiki]: https://github.com/FasterXML/jackson-module-scala/wiki
