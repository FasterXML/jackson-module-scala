package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.module.scala.BaseSpec

class DuplicateKeySavingMapTest extends BaseSpec {
  "DuplicateKeySavingMap" should "support toMap" in {
    val map = new DuplicateKeySavingMap[Int]()
    map.put(1, "one")
    map.put(2, "two")
    map.put(3, "x")
    map.put(3, "y")
    map.put(3, 1.234)
    val result = map.toMap
    result should have size 3
    result(1) shouldEqual "one"
    result(2) shouldEqual "two"
    result(3) shouldEqual Seq("x", "y", 1.234)
  }

  "DuplicateKeySavingMap" should "support toMutableMap" in {
    val map = new DuplicateKeySavingMap[Int]()
    map.put(1, "one")
    map.put(2, "two")
    map.put(3, "x")
    map.put(3, "y")
    map.put(3, 1.234)
    val result = map.toMutableMap
    result should have size 3
    result(1) shouldEqual "one"
    result(2) shouldEqual "two"
    result.remove(3) shouldEqual Some(Seq("x", "y", 1.234))
    result.get(3) shouldBe empty
  }
}
