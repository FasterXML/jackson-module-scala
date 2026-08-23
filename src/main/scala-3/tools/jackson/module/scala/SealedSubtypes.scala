package tools.jackson.module.scala

import scala.quoted.*

/**
 * Captures the implementations of a `sealed` hierarchy at compile time, so that Scala 3 can enumerate
 * one the way Scala 2 does through scala-reflect.
 *
 * Derive it on the base and the hierarchy is marked, exactly as extending
 * [[SealedPolymorphismSupport]] marks one - the two produce identical JSON:
 *
 * {{{
 * sealed trait Animal derives SealedSubtypes
 * case class Dog(name: String) extends Animal
 * case object Unknown extends Animal
 *
 * // {"@type":"Dog","name":"rex"}
 * }}}
 *
 * Deriving buys what Scala 3 cannot work out for itself. The compiler rejects a base that is not
 * `sealed`, and an implementation is found from the table rather than from where it was declared, so
 * one declared somewhere the marker alone could not reach is still read back, and a name claimed by
 * two implementations is reported for the whole hierarchy at once.
 *
 * @since 3.3.0
 */
trait SealedSubtypes[T] {
  /** Every implementation of the hierarchy, with the instance of each one that is an object. */
  def subtypes: Seq[(Class[?], Option[AnyRef])]
}

object SealedSubtypes {

  inline def derived[T]: SealedSubtypes[T] = ${ derivedImpl[T] }

  private def derivedImpl[T: Type](using Quotes): Expr[SealedSubtypes[T]] = {
    import quotes.reflect.*

    val root = TypeRepr.of[T].dealias.typeSymbol
    if (!root.flags.is(Flags.Sealed)) {
      report.errorAndAbort(
        s"${root.fullName} is not sealed. SealedSubtypes can only be derived for a sealed hierarchy, " +
          "since only then are all of its implementations known.")
    }

    // an abstract class or trait part way down holds no value of its own, but what is below it
    // still belongs to the hierarchy; a concrete one is both
    def implementations(symbol: Symbol): List[Symbol] = symbol.children.flatMap { child =>
      val below = if (child.flags.is(Flags.Sealed)) implementations(child) else Nil
      val itself = if (child.flags.is(Flags.Abstract) || child.flags.is(Flags.Trait)) Nil else List(child)
      itself ++ below
    }

    val found = implementations(root)
    if (found.isEmpty) report.errorAndAbort(s"${root.fullName} has no implementations to derive from")

    val entries = found.map { child =>
      val clazz = Literal(ClassOfConstant(child.typeRef)).asExprOf[Class[?]]
      if (child.flags.is(Flags.Module)) {
        val instance = Ref(child.companionModule).asExprOf[Any]
        '{ ($clazz, Some($instance.asInstanceOf[AnyRef])) }
      } else {
        '{ ($clazz, None) }
      }
    }

    '{
      new SealedSubtypes[T] {
        override def subtypes: Seq[(Class[?], Option[AnyRef])] = Seq(${ Varargs(entries) }*)
      }
    }
  }
}
