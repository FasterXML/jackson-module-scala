package com.fasterxml.jackson.module.scala.deser

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import com.fasterxml.jackson.module.scala.{JacksonModule, DefaultScalaModule}
import com.fasterxml.jackson.databind.{ObjectMapper, DeserializationConfig, JavaType, AbstractTypeResolver}
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.fasterxml.jackson.databind.`type`.MapLikeType

class UntypedObjectDeserializerTest extends DeserializerTest with FlatSpec with ShouldMatchers {
  def module = DefaultScalaModule

  val jsonString = """{"sKey":"sValue","mKey":{"mData":"mValue"}}"""

  behavior of "UntypedObjectDeserializer"

  it should "deserialize to an immutable.Map by default" in {
    val mapValue = deserialize[Map[String,Any]](jsonString)
    mapValue should contain key "sKey"
    mapValue("sKey") should be ("sValue")
    mapValue("mKey") match {
      case m: collection.immutable.Map[String,Any] =>
        m should contain key "mData"
        m("mData") should be ("mValue")
      case x => fail("Incorrect map type " + x.getClass)
    }
  }

  it should "honor AbstractTypeResolvers" in {
    object ATR extends AbstractTypeResolver {
      override def findTypeMapping(config: DeserializationConfig, javaType: JavaType) = {
        val result = Some(javaType) collect {
          case mapLikeType: MapLikeType =>
            if (javaType.getRawClass.equals(classOf[collection.Map[_,_]])) {
              javaType.narrowBy(classOf[collection.immutable.TreeMap[_,_]])
            } else null
        }
        result.orNull
      }
    }

    object AtrModule extends JacksonModule {
      this += (_ addAbstractTypeResolver ATR)
    }

    val atrMapper = new ObjectMapper with ScalaObjectMapper
    atrMapper.registerModule(DefaultScalaModule)
    atrMapper.registerModule(AtrModule)

    val mapValue = atrMapper.readValue[Map[String,Any]](jsonString)

    mapValue should contain key "sKey"
    mapValue("sKey") should be ("sValue")
    mapValue("mKey") match {
      case m: collection.immutable.TreeMap[String,Any] =>
        m should contain key "mData"
        m("mData") should be ("mValue")
      case x => fail("Incorrect map type " + x.getClass)
    }

  }


}
