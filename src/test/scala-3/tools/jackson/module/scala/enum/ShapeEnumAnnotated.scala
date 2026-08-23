package tools.jackson.module.scala.`enum`

import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(Array(
  new JsonSubTypes.Type(value = classOf[ShapeEnumAnnotated.Circle], name = "Circle"),
  new JsonSubTypes.Type(value = classOf[ShapeEnumAnnotated.Rectangle], name = "Rectangle"),
  new JsonSubTypes.Type(value = classOf[ShapeEnumAnnotated.Triangle], name = "Triangle")
))
enum ShapeEnumAnnotated:
  case Circle(radius: Double)
  case Rectangle(width: Double, height: Double)
  case Triangle(base: Double, height: Double)

case class ShapeEnumAnnotatedHolder(shape: ShapeEnumAnnotated)
