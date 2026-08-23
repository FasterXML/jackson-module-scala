package tools.jackson.module.scala

import scala.quoted.*

/**
 * Captures, at compile time, the type arguments the JVM erases.
 *
 * A generic signature keeps a reference type - `Option[String]` is still `Option<String>` at runtime -
 * but a Scala primitive is erased to `Object`, so `Option[Long]` is indistinguishable from
 * `Option[Int]`, and a small JSON number is read as an `Integer` a `Long` field cannot hold.
 */
trait ScalaTypeInfo[T] {
  /** Field name to the type argument the JVM erased, for the fields where one was erased. */
  def erasedTypeArguments: Seq[(String, Class[?])]
}

object ScalaTypeInfo {

  inline def derived[T]: ScalaTypeInfo[T] = ${ derivedImpl[T] }

  private def derivedImpl[T: Type](using Quotes): Expr[ScalaTypeInfo[T]] = {
    import quotes.reflect.*

    val target = TypeRepr.of[T].dealias.typeSymbol
    val params = target.primaryConstructor.paramSymss.flatten.filterNot(_.isTypeParam)

    // Walk to the innermost content type, which is the one the module replaces: for a Map that is
    // the value, so `Map[String, Long]` is reached and `Map[Long, String]` is deliberately not -
    // there the primitive is the key, and naming it would replace the value type instead and turn a
    // field that merely loses its key type into one that cannot be read at all.
    def contentLeaf(tpe: TypeRepr): TypeRepr = tpe.dealias match {
      case AppliedType(_, args) if args.nonEmpty => contentLeaf(args.last)
      case leaf => leaf
    }

    // only a primitive is lost: a reference type argument survives in the generic signature, and
    // Jackson reads it from there without any help
    def erasedArgument(tpe: TypeRepr): Option[TypeRepr] = tpe.dealias match {
      case AppliedType(_, _) => Some(contentLeaf(tpe)).filter(isPrimitive)
      case _ => None
    }

    def isPrimitive(tpe: TypeRepr): Boolean = tpe.classSymbol.exists { symbol =>
      Set("scala.Int", "scala.Long", "scala.Short", "scala.Byte", "scala.Double", "scala.Float",
        "scala.Boolean", "scala.Char").contains(symbol.fullName)
    }

    val entries = params.flatMap { param =>
      val paramType = target.typeRef.memberType(param).dealias
      erasedArgument(paramType).map { argument =>
        val name = Expr(param.name)
        val clazz = Literal(ClassOfConstant(argument)).asExprOf[Class[?]]
        '{ ($name, $clazz) }
      }
    }

    '{
      new ScalaTypeInfo[T] {
        override def erasedTypeArguments: Seq[(String, Class[?])] = Seq(${ Varargs(entries) }*)
      }
    }
  }
}
