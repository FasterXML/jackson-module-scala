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
Bean][] properites.

The Scala Module supports serialization and limited deserialization of
Scala Case Classes, Sequences, Maps, Tuples, Options, and Enumerations.

# Usage

To use the Scala Module in Jackson, simply register it with the
ObjectMapper instance:

    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)

`DefaultScalaModule` is a Scala object that includes support for all
currently supported Scala data types. If only partial support is desired,
the component traits can be included individually:

    val module = new OptionModule with TupleModule {}
    val mapper = new ObjectMapper()
    mapper.registerModule(module)

Consult the Scaladoc for further details.

[Jackson]: http://jackson.codehaus.org/
[SAX]: http://www.saxproject.org/
[DOM]: http://www.w3.org/TR/DOM-Level-3-Core/
[JAXB]: http://jaxb.java.net/
[Jersey]: http://jersey.java.net/
[Java Bean]: http://www.oracle.com/technetwork/java/javase/documentation/spec-136004.html
[Scala]: http://www.scala-lang.org/