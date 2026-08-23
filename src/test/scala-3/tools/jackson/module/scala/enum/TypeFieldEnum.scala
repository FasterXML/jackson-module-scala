package tools.jackson.module.scala.`enum`

// a parameterized case carrying a field of its own actually called `type`
enum TypeFieldEnum {
  case Typed(`type`: String)
  case Untyped
}

case class TypeFieldHolder(value: TypeFieldEnum)
