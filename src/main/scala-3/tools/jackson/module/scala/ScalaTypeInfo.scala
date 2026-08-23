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

    // only a primitive is lost: a reference type argument survives in the generic signature, and
    // Jackson reads it from there without any help
    def erasedArgument(tpe: TypeRepr): Option[TypeRepr] = tpe.dealias match {
      case AppliedType(_, args) => args.map(_.dealias).find(isPrimitive)
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
