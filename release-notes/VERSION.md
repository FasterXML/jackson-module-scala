# Version: 2.13.0 (Snapshot only)

This patch release tracks Jackson 2.13. The 2.13 series has full support for
Scala 2.11 and 2.12 and 2.13. Java 8 is the minimum supported Java version.

No functionality changes but the experimental package has been removed. The classes were moved in release 2.10.0 and experimental package was just kept to ease the transition.

# Version: 2.12.2 (Snapshot only)

## Fixes

* [[gh-495]](https://github.com/FasterXML/jackson-module-scala/issues/495) Fix regression since v2.12.0 where Scala objects (as opposed to case objects) were not serializing correctly.
* [[gh-497]](https://github.com/FasterXML/jackson-module-scala/issues/497) allow DescriptorCache to be replaced with custom implementation.

# Version: 2.12.1

No functionality changes but ScalaObjectMapper has been deprecated because it relies on `Manifest`s and these are not supported in Scala3. A number of other code changes have been made to try to get the code to compile in Scala3 but there is still a lot of test code that doesn't compile.
  
# Version: 2.12.0

This patch release tracks Jackson 2.12. The 2.12 series has full support for
Scala 2.11 and 2.12 and 2.13. Java 8 is now the minimum supported Java version. Scala 2.11 is now the minimum supported Scala version.

## Fixes

* [[gh-370]](https://github.com/FasterXML/jackson-module-scala/issues/370) Support jackson feature @JsonMerge (added after rc2 release). Thanks to Helder Pereira.
* [[gh-449]](https://github.com/FasterXML/jackson-module-scala/issues/449) Remove jackson-module-paranamer dependency. Scala 2.11 releases use paranamer directly still. Scala 2.12 and 2.13 releases no longer use paranamer.
* [[gh-455]](https://github.com/FasterXML/jackson-module-scala/issues/455) get ScalaAnnotationIntrospector to ignore non-Scala classes.
* [[gh-462]](https://github.com/FasterXML/jackson-module-scala/issues/462) Unable to deserialize Seq or Map with AS_EMPTY null handling
* [[gh-466]](https://github.com/FasterXML/jackson-module-scala/issues/466) Add support for WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED
* [[gh-467]](https://github.com/FasterXML/jackson-module-scala/issues/467) Serializer for Scala Iterable/Iterator converts to Java Collection - avoid this conversion
* [[gh-480]](https://github.com/FasterXML/jackson-module-scala/issues/480) Drop Scala 2.10 support

# Version: 2.11.4

There are no new fixes in this release; it serves to track the 2.11.4 release
of the upstream Jackson projects.

# Version: 2.11.3

## Fixes

* [[gh-472]](https://github.com/FasterXML/jackson-module-scala/issues/472)
  `Either` deserializers `Option[T]` with value None as `null`. Thanks to Domantas Petrauskas.

# Version: 2.11.2

## Fixes

* [[gh-454]](https://github.com/FasterXML/jackson-module-scala/issues/454)
  Jackson module scala potentially breaks serialization for swagger Model.

# Version: 2.11.1

There are no new fixes in this release; it serves to track the 2.11.1 release
of the upstream Jackson projects.

# Version: 2.11.0

This patch release tracks Jackson 2.11. The 2.11 series has full support for
Scala 2.10, 2.11 and 2.12 and 2.13.

## Fixes

* [[gh-87]](https://github.com/FasterXML/jackson-module-scala/issues/87)
  support default values in case class parameter lists
* [[gh-445]](https://github.com/FasterXML/jackson-module-scala/issues/445) fix NullPointerException that can happen
  with some case classes (caused by gh-87 fix) - issue still appears in 2.11.0.rc1 but is fixed in 2.11.0

# Version: 2.10.5

There are no new fixes in this release; it serves to track the 2.10.5 release
of the upstream Jackson projects.

# Version: 2.10.4

There are no new fixes in this release; it serves to track the 2.10.4 release
of the upstream Jackson projects.

# Version: 2.10.3

## Fixes

* [[gh-218]](https://github.com/FasterXML/jackson-module-scala/issues/218)
  Fix issue with serialization of case class with overridden attributes

# Version: 2.10.2

There are no new fixes in this release; it serves to track the 2.10.2 release
of the upstream Jackson projects.

# Version: 2.10.1

There are no new fixes in this release; it serves to track the 2.10.1 release
of the upstream Jackson projects.

# Version: 2.10.0

This patch release tracks Jackson 2.10. The 2.10 series adds full support for
Scala 2.13 while maintaining support for 2.10, 2.11 and 2.12.

The addition of a SymbolModule to DefaultScalaModule might lead to some behaviour changes for some users.

## Fixes

* [[gh-432]](https://github.com/FasterXML/jackson-module-scala/issues/432)
  move experimental classes to main package
* [[gh-pr-264]](https://github.com/FasterXML/jackson-module-scala/pull/264)
  Add SymbolModule to support Scala symbols
* [[gh-399]](https://github.com/FasterXML/jackson-module-scala/issues/399)
  JsonScalaEnumeration annotation not picked up when using a Mixin
* [[gh-429]](https://github.com/FasterXML/jackson-module-scala/issues/429)
  Serialization behavior of case objects is different when using scala 2.13
* [[databind-2422]](https://github.com/FasterXML/jackson-databind/issues/2422)
  `scala.collection.immutable.ListMap` fails to serialize since 2.9.3 (fix in jackson-databind that is useful for Scala developers)

# Version: 2.9.10

## Fixes

* [[gh-399]](https://github.com/FasterXML/jackson-module-scala/issues/399)
  JsonScalaEnumeration annotation not picked up when using a Mixin
* [[gh-429]](https://github.com/FasterXML/jackson-module-scala/issues/429)
  Serialization behavior of case objects is different when using scala 2.13
* [[databind-2422]](https://github.com/FasterXML/jackson-databind/issues/2422)
  `scala.collection.immutable.ListMap` fails to serialize since 2.9.3 (fix in jackson-databind that is useful for Scala developers)

# Version: 2.9.9

The first release to support Scala 2.13. Thanks to Adriaan Moors and Seth Tisue.

## Fixes

* [[gh-401]](https://github.com/FasterXML/jackson-module-scala/issues/401)
  Deserialization of inner Enumeration class fails. Thanks to Stefan Endrullis.
* [[gh-400]](https://github.com/FasterXML/jackson-module-scala/issues/400)
  Remove unused http resolver in build

# Version: 2.9.8

This minor release included support for Scala 2.13 milestone releases.

# Version: 2.9.7

This minor release included support for Scala 2.13 milestone releases.

## Fixes

* [[gh-372]](https://github.com/FasterXML/jackson-module-scala/issues/372)
  Remove dependency on scala-reflect
* [[gh-353]](https://github.com/FasterXML/jackson-module-scala/issues/353)
  path not computed on collection
* [[gh-314]](https://github.com/FasterXML/jackson-module-scala/issues/314)
  Serialize Some(null)  
* [[gh-299]](https://github.com/FasterXML/jackson-module-scala/issues/299)
  unable to handle Option of a case class

# Version: 2.9.6

This minor release included support for Scala 2.13 milestone releases.
Thanks to Kenji Yoshida.

# Version: 2.9.5

## Fixes

* [[gh-287]](https://github.com/FasterXML/jackson-module-scala/issues/287)
  Null pointer exception when trying to deserialize into Either[]. Thanks to staffanstockholm.
* [[gh-pr-324]](https://github.com/FasterXML/jackson-module-scala/pull/324) 
  Fix bean accessors being used for field names. Thanks to Nick Telford.

# Version: 2.9.4

## Fixes

* [[gh-346]](https://github.com/FasterXML/jackson-module-scala/issues/346)
  `@JsonInclude` content inclusion does not work. Thanks to brharrington.
  
# Version: 2.9.3

There are no new fixes in this release; it serves to track the 2.9.3 release
of the upstream Jackson projects.

# Version: 2.9.2

There are no new fixes in this release; it serves to track the 2.9.2 release
of the upstream Jackson projects.

# Version: 2.9.1

## Fixes

* [[gh-338]](https://github.com/FasterXML/jackson-module-scala/issues/338)
  Deserialization of JSON object with null values into a `Map[String, JsonNode]` causes a NullPointerException. Thanks to David Pratt.
* [[gh-287]](https://github.com/FasterXML/jackson-module-scala/issues/287)
  Null pointer exception when trying to deserialize into `Either[]`. Thanks to Jonathan Stearnes.

# Version: 2.9.0

This minor release tracks Jackson 2.9 and fixes a number of issues.

## Fixes

* [[gh-pr-324]](https://github.com/FasterXML/jackson-module-scala/pull/324)
  Fix bean accessors being used for field names. Thanks to Nick Telford.

# Versions 2.6.0 - 2.8.x

Version notes for these versions were not produced and will need to be
updated retrospectively.

Most releases were just to keep the jackson-module-scala release number
in sync with the rest of the jackson jars.

# Version: 2.5.0

This minor release tracks Jackson 2.5 and fixes a number of issues.

## Fixes

* [[gh-153]](https://github.com/FasterXML/jackson-module-scala/issues/153)
  Ramp up to guava 17.0

* [[gh-134]](https://github.com/FasterXML/jackson-module-scala/issues/134)
  Instantiation of a Scala class with a single val from a string fails

# Version: 2.4.4

## Fixes

* [[gh-149]](https://github.com/FasterXML/jackson-module-scala/issues/149)
  Use type information to deser `Option` (courtesy of @orac)

* [[gh-148]](https://github.com/FasterXML/jackson-module-scala/issues/148)
  Performance regression in 2.2.3

* [[gh-145]](https://github.com/FasterXML/jackson-module-scala/issues/145)
  readValue for `Map[String, Any]` or `List[Any]` is very slow

# Version: 2.4.3

## Fixes

* [[gh-157]](https://github.com/FasterXML/jackson-module-scala/pull/157)
  Use type information to deser `Option` (courtesy of @orac)

* [[gh-154]](https://github.com/FasterXML/jackson-module-scala/issues/154)
  Omitting `null` in `Map("key" -> None)`

# Version: 2.4.2

There are no new fixes in this release; it serves to track the 2.4.2 release
of the upstream Jackson projects.

# Version: 2.4.1

There are no new fixes in this release; it serves to track the 2.4.1 release
of the upstream Jackson projects.

# Version: 2.4.0

This minor release tracks Jackson 2.4. It is the first version of the
module to support Scala 2.11, and drops support for Scala 2.9. Support
for Scala 2.9 remains in the 2.3.x series, which will receive critical
bug fixes for the foreseeable future, but new features will only be
applied to 2.4.0 and beyond.

Release 2.4 also fixes a number of issues.

## Fixes

* [[gh-134]](https://github.com/FasterXML/jackson-module-scala/issues/134)
  Instantiation of a Scala class with a single val from a string fails

* [[gh-133]](https://github.com/FasterXML/jackson-module-scala/issues/133)
  2.11 build

* [[gh-132]](https://github.com/FasterXML/jackson-module-scala/pull/132)
  treat Option as CollectionLike in ScalaObjectMapper (courtesy of @wpalmeri)

# Version: 2.3.3

## Fixes

* [[gh-139]](https://github.com/FasterXML/jackson-module-scala/issues/139)
  Failed serialization of objects with collections of objects when using a custom `TypeResolverBuilder`

* [[gh-138]](https://github.com/FasterXML/jackson-module-scala/issues/138)
  `JsonTypeInfo` for classes inside a Seq

* [[gh-135]](https://github.com/FasterXML/jackson-module-scala/issues/135)
  `@JsonScalaEnumeration` doesn't apply inside an `Option`

# Version: 2.3.2

## Fixes

* [[gh-131]](https://github.com/FasterXML/jackson-module-scala/issues/131)
  `Map` deserialization loses key type information

* [[gh-130]](https://github.com/FasterXML/jackson-module-scala/issues/130)
  jackson-module-scala don't include the license file

* [[gh-127]](https://github.com/FasterXML/jackson-module-scala/issues/127)
  `BigDecimal` fails to deserialize an integer

* [[gh-126]](https://github.com/FasterXML/jackson-module-scala/issues/126)
  Serializing `Option` with `enableDefaultTyping` fails

* [[gh-125]](https://github.com/FasterXML/jackson-module-scala/issues/125)
  `JsonTypeInfo` on `Option` doesn't work

* [[gh-124]](https://github.com/FasterXML/jackson-module-scala/issues/124)
  `@JsonValue` annotation doesn't work with Scala `val` definitions

* [[gh-120]](https://github.com/FasterXML/jackson-module-scala/issues/120)
  Serializing a concrete class inheriting a trait randomly fails

# Version: 2.3.1

## Fixes

* [[gh-116]](https://github.com/FasterXML/jackson-module-scala/issues/116)
  Deserializing Iterable

* [[gh-105]](https://github.com/FasterXML/jackson-module-scala/issues/105)
  Deserialization fails for Array[T] in case class

# Version: 2.3.0

This patch release tracks Jackson 2.3 and fixes a number of issues.

## Fixes

* [[gh-103]](https://github.com/FasterXML/jackson-module-scala/issues/103)
  (Regression) Serialization of a class containing Option[JsonNode] fails

* [[gh-102]](https://github.com/FasterXML/jackson-module-scala/issues/102)
  (Regression) JsonMappingException Argument of constructor has no property name annotation

* [[gh-101]](https://github.com/FasterXML/jackson-module-scala/issues/101)
  version 2.2.3 can't deserialize some class that verision 2.2.2 can

* [[gh-100]](https://github.com/FasterXML/jackson-module-scala/issues/100)
  Deserializing SortedSets
  
## Known issues

[|#105| Deserialization fails for Array[T] in case class](https://github.com/FasterXML/jackson-module-scala/issues/105)

This issue should have been resolved in 2.3, but due to a release error did not get merged into the release. It is
fixed in 2.3.1.

# Version: 2.2.3

## Fixes

* [[gh-98]](https://github.com/FasterXML/jackson-module-scala/issues/98)
  OSGI Export

* [[gh-97]](https://github.com/FasterXML/jackson-module-scala/issues/97)
  Non symmetric serialize/deserialize behavior

* [[gh-95]](https://github.com/FasterXML/jackson-module-scala/issues/95)
  Error deserializing pojo with multi-map when guava and scala modules are used

* [[gh-93]](https://github.com/FasterXML/jackson-module-scala/issues/93)
  Enumerations and Java enums deserialize into strings when being used as Scala map keys

* [[gh-91]](https://github.com/FasterXML/jackson-module-scala/issues/91)
  setPropertyNamingStrategy results in NullPointerException when serializing

* [[gh-89]](https://github.com/FasterXML/jackson-module-scala/issues/89)
  Customizing untyped object deserialization

* [[gh-88]](https://github.com/FasterXML/jackson-module-scala/issues/88)
  Conflicting setter definitions for property

* [[gh-85]](https://github.com/FasterXML/jackson-module-scala/issues/85)
  Java style getters still broken

* [[gh-83]](https://github.com/FasterXML/jackson-module-scala/issues/83)
  value update problem

# Version: 2.2.2

## Fixes

* [[gh-80]](https://github.com/FasterXML/jackson-module-scala/issues/80)
  Option fields not marked as optional in JSON schema

* [[gh-81]](https://github.com/FasterXML/jackson-module-scala/pull/81)
  Option JsonSchema generation with jackson-module-jsonSchema

# Version: 2.2.1

This release fixes a number of bugs in the 2.2.0 release. Many thanks to
[David Pratt](https://github.com/dpratt) for significant contributions to this release.

## Fixes

* [[gh-71]](https://github.com/FasterXML/jackson-module-scala/issues/71)
  Support private fields in scala classes when default values are present

* [[gh-73]](https://github.com/FasterXML/jackson-module-scala/issues/73)
  "Conflicting getter definitions" with @BeanProperty

* [[gh-74]](https://github.com/FasterXML/jackson-module-scala/pull/74)
  Serialization of nested case classes, from [David Pratt](https://github.com/dpratt)

* [[gh-75]](https://github.com/FasterXML/jackson-module-scala/pull/75)
  General fixes for less-common field types, from [David Pratt](https://github.com/dpratt)

* [[gh-76]](https://github.com/FasterXML/jackson-module-scala/pull/76)
  Improvement to ScalaObjectMapper, from [David Pratt](https://github.com/dpratt)

# Version: 2.2.0

Property detection has been completely rewritten in 2.2. In many common cases,
`@JsonProperty` annotations will be unnecessary, as the property names will
be read from debugging information contained in the jar file.

New users should be careful not to remove 'vars' debugging information from
their jar files. `javac` includes it by default, so this will not affect
most users, but for size-sensitive projects it is often omitted in not-debug
scenarios. Doing so will break the Scala module which relies on the
presence of constructor parameter name debugging information to correctly
detect Scala property names.

## Known issues

[|#73| "Conflicting getter definitions" with @BeanProperty](https://github.com/FasterXML/jackson-module-scala/issues/73):

In cases where `@BeanProperty` annotations are used, the annotations conflict
with the default properties detected from the class. The current workaround
is to create a custom `JacksonModule` that does not include the `ScalaClassIntrospectorModule`.


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
