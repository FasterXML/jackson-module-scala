package tools.jackson.module.scala

import scala.quoted.*

/**
 * Captures, at compile time, the type arguments the JVM erases.
 *
 * A generic signature keeps a reference type - `Option[String]` is still `Option<String>` at runtime -
 * but a Scala primitive is erased to `Object`, so `Option[Long]` is indistinguishable from
 * `Option[Int]`, and a small JSON number is read as an `Integer` a `Long` field cannot hold.
 *
 * Derive it on a case class and every constructor parameter that loses a primitive that way is read
 * with its declared type instead, wherever in the type the primitive sits: the content of an `Option`
 * or a collection, the key or the value of a `Map`, a slot of a tuple or an `Either`, the argument of
 * a generic case class, or any nesting of those.
 *
 * {{{
 * case class Erased(aLong: Option[Long], byId: Map[Long, String], pairs: Seq[(String, Long)]) derives ScalaTypeInfo
 * }}}
 *
 * Derive it on an `enum` or on the base of a `sealed` hierarchy and every case or implementation is
 * covered by that one clause - a `derives` cannot be written on an enum case, and a hierarchy is
 * usually easier to mark once at its root:
 *
 * {{{
 * enum Shape derives ScalaTypeInfo:
 *   case Circle(radius: Option[Long])
 *   case Dot
 * }}}
 *
 * What is captured describes the constructor parameters only. A `var` set after construction is
 * typed by its setter, which this does not reach; annotate one with `@JsonDeserialize` instead. A
 * parameter whose type mentions a type parameter of the class (`case class Box[T](v: Option[T])`) is
 * left to Jackson, which knows the argument from the type it was asked to read.
 *
 * @since 3.3.0
 */
trait ScalaTypeInfo[T] {
  /**
   * The full type of every constructor parameter that loses a primitive to erasure, keyed by the
   * class declaring the parameter and its name. Deliberately not public: it describes what the module
   * puts back, and that is the module's to change.
   */
  private[scala] def erasedFields: Seq[(Class[?], String, ScalaTypeInfo.TypeShape)]
}

object ScalaTypeInfo {

  /**
   * A type as the compiler saw it, with the arguments the JVM would drop: the raw class and one shape
   * per type argument. A primitive is its primitive class (`classOf[Long]` is `long`), so the shape
   * for `Map[Long, Seq[Int]]` is `Map` applied to `long` and `Seq` applied to `int`.
   *
   * Built by what `derived` generates, which is expanded where the class is declared and so can only
   * reach what is public. Nothing else should construct one.
   */
  final case class TypeShape(rawClass: Class[?], typeArguments: Seq[TypeShape])

  /**
   * Called by what `derived` generates, for the same reason [[TypeShape]] is public. Nothing else
   * should call it.
   */
  def derivedFrom[T](erased: Seq[(Class[?], String, TypeShape)]): ScalaTypeInfo[T] = new ScalaTypeInfo[T] {
    override private[scala] def erasedFields: Seq[(Class[?], String, TypeShape)] = erased
  }

  inline def derived[T]: ScalaTypeInfo[T] = ${ derivedImpl[T] }

  private def derivedImpl[T: Type](using Quotes): Expr[ScalaTypeInfo[T]] = {
    import quotes.reflect.*

    val root = TypeRepr.of[T].dealias.typeSymbol

    val PrimitiveNames = Set("scala.Int", "scala.Long", "scala.Short", "scala.Byte", "scala.Double",
      "scala.Float", "scala.Boolean", "scala.Char")
    val UntypedNames = Set("scala.Any", "scala.AnyRef", "scala.AnyVal", "scala.Nothing", "scala.Null",
      "java.lang.Object")
    val arraySymbol = TypeRepr.of[Array[?]].typeSymbol
    val objectClass = Literal(ClassOfConstant(TypeRepr.of[Object])).asExprOf[Class[?]]

    def isPrimitive(tpe: TypeRepr): Boolean = tpe.classSymbol.exists(s => PrimitiveNames.contains(s.fullName))

    // What a `derives` on this type describes: the type itself, or for an enum or a sealed base every
    // concrete case or implementation below it. An abstract class or trait part way down holds no
    // value of its own, but what is below it still belongs to the hierarchy.
    def described(symbol: Symbol): List[Symbol] = {
      val isBase = symbol.flags.is(Flags.Sealed) || symbol.flags.is(Flags.Enum)
      val holdsValues = !(symbol.flags.is(Flags.Abstract) || symbol.flags.is(Flags.Trait))
      val below = if (isBase) symbol.children.filter(_.isClassDef).flatMap(described) else Nil
      val itself = if (holdsValues) List(symbol) else Nil
      itself ++ below
    }

    def leaf(tpe: TypeRepr): Expr[TypeShape] =
      '{ TypeShape(${ Literal(ClassOfConstant(tpe)).asExprOf[Class[?]] }, Seq.empty) }

    /**
     * The shape of a type, and whether a primitive was found among its arguments, or `None` where
     * the type reaches something only known at runtime - a type parameter of the class, a wildcard,
     * an abstract type - and so cannot be described in full.
     */
    def shape(tpe: TypeRepr, nested: Boolean): Option[(Expr[TypeShape], Boolean)] = tpe.dealias match {
      case _: TypeBounds | _: ParamRef => None
      // classSymbol would answer with the bound of a type parameter or an abstract type, which is not
      // what the field holds
      case named if named.typeSymbol.isTypeParam || named.typeSymbol.isAbstractType => None
      case applied @ AppliedType(_, args) =>
        applied.classSymbol match {
          case Some(symbol) if symbol == arraySymbol =>
            // an array keeps its element type on the JVM, `long[]` included, so it is a leaf - unless
            // what it holds cannot be named
            if (args.forall(arg => shape(arg, nested = true).isDefined)) Some((leaf(applied), false)) else None
          case Some(_) =>
            val argShapes = args.map(arg => shape(arg, nested = true))
            if (argShapes.forall(_.isDefined)) {
              val (exprs, found) = argShapes.flatten.unzip
              val clazz = Literal(ClassOfConstant(applied)).asExprOf[Class[?]]
              Some(('{ TypeShape($clazz, Seq(${ Varargs(exprs) }*)) }, found.contains(true)))
            } else None
          case _ => None
        }
      case other =>
        other.classSymbol match {
          case Some(_) if isPrimitive(other) => Some((leaf(other), nested))
          case Some(symbol) if UntypedNames.contains(symbol.fullName) =>
            Some(('{ TypeShape($objectClass, Seq.empty) }, false))
          case Some(_) => Some((leaf(other), false))
          case _ => None
        }
    }

    val entries = described(root).flatMap { symbol =>
      val clazz = Literal(ClassOfConstant(symbol.typeRef)).asExprOf[Class[?]]
      val params = symbol.primaryConstructor.paramSymss.flatten.filterNot(_.isTypeParam)
      params.flatMap { param =>
        val paramType = symbol.typeRef.memberType(param).dealias
        // only a field that actually loses a primitive is described: a top-level primitive is a
        // primitive on the JVM too, and a reference type argument survives in the signature
        shape(paramType, nested = false).collect { case (typeShape, true) =>
          val name = Expr(param.name)
          '{ ($clazz, $name, $typeShape) }
        }
      }
    }

    '{ ScalaTypeInfo.derivedFrom[T](Seq(${ Varargs(entries) }*)) }
  }
}
