package com.fasterxml.jackson.module.scala.introspect

import com.fasterxml.jackson.databind.JavaType

object OrderingLocator {
  val ORDERINGS = Map(
    classOf[Unit] -> Ordering.Unit,
    classOf[Boolean] -> Ordering.Boolean,
    classOf[Byte] -> Ordering.Byte,
    classOf[Char] -> Ordering.Char,
    classOf[Short] -> Ordering.Short,
    classOf[Int] -> Ordering.Int,
    classOf[Long] -> Ordering.Long,
    classOf[Float] -> Ordering.Float,
    classOf[Double] -> Ordering.Double,
    classOf[BigInt] -> Ordering.BigInt,
    classOf[BigDecimal] -> Ordering.BigDecimal,
    classOf[String] -> Ordering.String
  )

  def locate(javaType: JavaType): Ordering[AnyRef] = {
    def matches(other: Class[_]) = other.isAssignableFrom(javaType.getRawClass)
    val ordering =
      ORDERINGS.find(e => matches(e._1)).map(_._2).getOrElse {
        if (matches(classOf[Option[_]])) {
          val delegate = locate(javaType.containedType(0))
          Ordering.Option(delegate)
        }
        else if (matches(classOf[Comparable[_]]))
          new Ordering[AnyRef] {
            def compare(x: AnyRef, y: AnyRef): Int = {
              x.asInstanceOf[Comparable[AnyRef]].compareTo(y)
            }
          }

        else throw new IllegalArgumentException("Unsupported value type: " + javaType.getRawClass.getCanonicalName)
      }

    ordering.asInstanceOf[Ordering[AnyRef]]
  }
}
