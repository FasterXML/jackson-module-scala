package com.fasterxml.jackson.module.scala

import scala.reflect.BeanProperty

/**
 */

class EnumContainer {

	@BeanProperty
	var day = Weekday.Fri
}