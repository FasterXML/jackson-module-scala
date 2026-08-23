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
 * The value written to `@type` is a derived name - the implementation's simple name, never a fully
 * qualified class name. On the way back in, a name is resolved only against types declared
 * alongside the base type (its package, or the object enclosing it) and only if the result is
 * actually a subtype of that base. Because the base is `sealed`, its implementations must be
 * declared in the same file, so that is exactly the set of legal implementations - a `@type` value
 * cannot name anything else on the classpath.
 *
 * @since 3.3.0
 */
trait SimplePolymorphismSupport
