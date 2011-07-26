package com.fasterxml.jackson.module.scala

import reflect.BeanProperty

/**
 */

class Bean {

	@BeanProperty
	var name: String = "Dave"

	@BeanProperty
	var age: Int = 23

	override def equals(that: Any) = that match {
		case other: Bean => name.equals(other.name) && age.equals(other.age)
		case _ => false
	}
}
