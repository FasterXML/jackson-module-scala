package tools.jackson.module.scala.`enum`

enum ResultEnum {
  case Ok(value: String)
  case Error(code: Int)
  case Pending
}

case class Result(result: ResultEnum)
