package com.fasterxml.jackson.module.scala.ser

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.fasterxml.jackson.module.scala.{DefaultScalaModule, JacksonModule}
import java.lang.Integer

/**
 * Undocumented class.
 */
@RunWith(classOf[JUnitRunner])
class OptionSerializerTest extends SerializerTest with FlatSpec with ShouldMatchers {

  lazy val module = DefaultScalaModule

  "An ObjectMapper with OptionSerializer" should "serialize an Option[Int]" in {
    val noneOption: Option[Int] = None
    serialize(Option(1)) should be ("1")
    serialize(Some(1)) should be ("1")
    serialize(noneOption) should be ("null")
  }

  it should "serialize an Option[String]" in {
    val noneOption: Option[String] = None
    serialize(Option("foo")) should be ("\"foo\"")
    serialize(Some("foo")) should be ("\"foo\"")
    serialize(noneOption) should be ("null")
  }

  it should "serialize an Option[java.lang.Integer]" in {
    val noneOption: Option[java.lang.Integer] = None
    val someInt: Option[java.lang.Integer] = Some(1)
    serialize(someInt) should be ("1")
    serialize(noneOption) should be ("null")
  }

  it should "serialize an Option[java.lang.Integer] when accessed on a class" in {
    case class Review(score: java.lang.Integer)
    val r1: Review = null
    val r2: Review = Review(1)
    def score1 = Option(r1) map { _.score }
    def score2 = Option(r2) map { _.score }
    serialize(score1) should be ("null")
    serialize(score2) should be ("1")
  }

}
