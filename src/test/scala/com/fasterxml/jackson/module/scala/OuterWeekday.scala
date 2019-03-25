package com.fasterxml.jackson.module.scala

object OuterWeekday {

  object InnerWeekday extends Enumeration {
    type InnerWeekday = Value
    val Mon, Tue, Wed, Thu, Fri, Sat, Sun = Value
  }

}
