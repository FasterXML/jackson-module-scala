package com.fasterxml.jackson.module.scala

import reflect.BeanProperty

/**
 */

class EnumContainer {

	@BeanProperty
	var day = Weekday.Fri
}