package tools.jackson.module.scala

/**
 * Marker trait that opts a Scala type hierarchy into automatic polymorphic serialization, without
 * the `@JsonTypeInfo` and `@JsonSubTypes` annotations that Jackson normally requires.
 *
 * Extend it from the base of a `sealed` hierarchy and every implementation gains a `@type` property
 * naming the implementation:
 *
 * {{{
 * sealed trait Animal extends SimplePolymorphismSupport
 * case class Dog(name: String) extends Animal
 * case object Unknown extends Animal
 *
 * // {"@type":"Dog","name":"rex"}
 * // {"@type":"Unknown"}
 * }}}
 *
 * The value written to `@type` is a derived name, never a fully qualified class name. An
 * implementation declared beside the base, or inside the base's companion, is named by its simple
 * name; one declared inside some other object keeps that object in its name, so two objects can
 * each hold an implementation called the same thing:
 *
 * {{{
 * sealed trait Dup extends SimplePolymorphismSupport
 * object Left  { case class Same(v: Int)    extends Dup }   // {"@type":"Left$Same","v":1}
 * object Right { case class Same(v: String) extends Dup }   // {"@type":"Right$Same","v":"x"}
 * }}}
 *
 * On the way back in, a name is resolved only against types declared alongside the base type - its
 * package, the object enclosing it, or its own companion - and only if the result is actually a
 * subtype of that base. Because the base is `sealed`, its implementations must be declared in the
 * same file, so that is exactly the set of legal implementations: a `@type` value cannot name
 * anything else on the classpath.
 *
 * The base is therefore expected to be `sealed`. Scala does not record that on the JVM, so it
 * cannot be checked directly; instead every implementation is checked, as its serializer is built,
 * to be findable again under the name it would be written with. One declared outside the base's
 * package - which `sealed` would have prevented - or one whose derived name is already taken by an
 * implementation declared closer to the base is rejected there and then, rather than written out as
 * JSON that could never be read back.
 *
 * @since 3.3.0
 */
trait SimplePolymorphismSupport
