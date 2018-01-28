package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.module.scala.{JacksonModule, DefaultScalaModule}
import com.fasterxml.jackson.databind.{ObjectMapper, DeserializationConfig, JavaType, AbstractTypeResolver}
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.fasterxml.jackson.databind.`type`.{TypeFactory, MapLikeType}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class UntypedObjectDeserializerTest extends DeserializerTest {
  def module = DefaultScalaModule

  val jsonString = """{"sKey":"sValue","mKey":{"mData":"mValue"}}"""

  behavior of "UntypedObjectDeserializer"

  it should "deserialize to an immutable.Map by default" in {
    val mapValue = deserialize[Map[String,Any]](jsonString)
    mapValue should contain key "sKey"
    mapValue("sKey") should be ("sValue")
    val mKeyValue = mapValue("mKey")
    mKeyValue shouldBe a [collection.immutable.Map[_,_]]
    val typedMKeyValue = mKeyValue.asInstanceOf[collection.immutable.Map[String,Any]]
    typedMKeyValue should contain key "mData"
    typedMKeyValue("mData") shouldBe "mValue"
  }

  it should "honor AbstractTypeResolvers" in {
    object ATR extends AbstractTypeResolver {
      override def findTypeMapping(config: DeserializationConfig, javaType: JavaType) = {
        val result = Some(javaType) collect {
          case mapLikeType: MapLikeType =>
            if (javaType.getRawClass.equals(classOf[collection.Map[_,_]])) {
              config.getTypeFactory.constructSpecializedType(javaType, classOf[collection.immutable.TreeMap[_,_]])
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
    val mKeyValue = mapValue("mKey")
    mKeyValue shouldBe a [collection.immutable.TreeMap[_,_]]
    val typedMKeyValue = mKeyValue.asInstanceOf[collection.immutable.TreeMap[String,Any]]
    typedMKeyValue should contain key "mData"
    typedMKeyValue("mData") shouldBe "mValue"

  }


}
