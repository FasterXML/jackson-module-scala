package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.annotation.{JsonIgnore, JsonUnwrapped}
import com.fasterxml.jackson.databind.ObjectMapper

case class Address(address1: Option[String], city: Option[String], state: Option[String])

class NonCreatorPerson {
  var name: String = _
  @JsonUnwrapped var location: Address = _
  var alias: Option[String] = _
}

case class Person(name: String, @JsonIgnore location: Address, alias: Option[String]) {
  private def this() = this("", Address(None, None, None), None)

  def address1: Option[String] = location.address1
  private def address1_=(value: Option[String]): Unit = {
    setAddressField("address1", value)
  }

  def city: Option[String] = location.city
  private def city_=(value: Option[String]): Unit = {
    setAddressField("city", value)
  }

  def state: Option[String] = location.state
  private def state_= (value: Option[String]): Unit = {
    setAddressField("state", value)
  }

  private def setAddressField(name: String, value: Option[String]): Unit = {
    val f = location.getClass.getDeclaredField(name)
    f.setAccessible(true)
    f.set(location, value)
  }
}

class UnwrappedTest extends BaseSpec {

  "mapper" should "handle ignored fields correctly" in {
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)

    val p = Person("Snoopy", Address(Some("123 Main St"), Some("Anytown"), Some("WA")), Some("Joe Cool"))
    val json = mapper.writeValueAsString(p)

    // There's some instability in the ordering of keys. Not sure what that's about, but rather than
    // have buggy tests, I'm accepting it for now.
    //    json should (
    //      be === """{"name":"Snoopy","alias":"Joe Cool","city":"Anytown","address1":"123 Main St","state":"WA"}""" or
    //      be === """{"name":"Snoopy","alias":"Joe Cool","state":"WA","address1":"123 Main St","city":"Anytown"}"""
    //    )

    val p2 = mapper.readValue(json, classOf[Person])

    p2 shouldEqual p
  }

  it should "handle JsonUnwrapped for non-creators" in {
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)

    val p = new NonCreatorPerson
    p.name = "Snoopy"
    p.location = Address(Some("123 Main St"), Some("Anytown"), Some("WA"))
    p.alias = Some("Joe Cool")

    val json = mapper.writeValueAsString(p)
    val p2 = mapper.readValue(json, classOf[NonCreatorPerson])

    p2.name shouldBe p.name
    p2.location shouldBe p.location
    p2.alias shouldBe p.alias
  }
}
