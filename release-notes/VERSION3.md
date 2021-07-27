Check VERSION.md for jackson-module-scala 2.x release information. This file tracks jackson-module-scala 3.x.

# 3.0.0 (release not yet scheduled)

This patch release tracks Jackson 3.0.0. The 2.13 series has full support for Scala 2.11, 2.12, 2.13 and 3.0. There are many changes in related jackson libraries
that affect all jackson users. jackson-module-scala changes are less significant. The most notable change is the addition of ScalaModule builder. Users who don't use DefaultScalaModule may need to change to using ScalaModule builder.

* [[gh-525]](https://github.com/FasterXML/jackson-module-scala/issues/525) Add ScalaModule builder 
* [[gh-531]](https://github.com/FasterXML/jackson-module-scala/issues/531) Remove DefaultRequiredAnnotationIntrospector as part of dropping support for jackson-module-jsonSchema
