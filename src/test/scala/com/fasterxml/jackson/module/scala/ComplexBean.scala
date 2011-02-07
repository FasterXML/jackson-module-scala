package com.fasterxml.jackson.module.scala

import reflect.BeanProperty

class ComplexBean {

	@BeanProperty
	var bean: Bean = new Bean

	@BeanProperty
	var map: Map[String, String] = Map("key" -> "value")

	@BeanProperty
	var favoriteNumbers: List[Int] = (1 to 3).toList

	override def equals(that: Any) = that match {
		case other: ComplexBean =>
			bean.equals(other.bean) && map.equals(other.map) && favoriteNumbers.equals(other.favoriteNumbers)
		case _ => false
	}
}