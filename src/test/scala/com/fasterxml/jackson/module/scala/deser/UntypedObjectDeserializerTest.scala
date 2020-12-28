package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.`type`.MapLikeType
import com.fasterxml.jackson.databind.{AbstractTypeResolver, DeserializationConfig, JavaType, ObjectMapper}
import com.fasterxml.jackson.module.scala.{DefaultScalaModule, JacksonModule}

class UntypedObjectDeserializerTest extends DeserializerTest {
  def module: JacksonModule = DefaultScalaModule

  val jsonString = """{"sKey":"sValue","mKey":{"mData":"mValue"}}"""

  behavior of "UntypedObjectDeserializer"

  it should "deserialize to an immutable.Map by default" in {
    val mapValue = deserialize(jsonString, classOf[Map[String,Any]])
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

    val atrMapper = new ObjectMapper
    atrMapper.registerModule(DefaultScalaModule)
    atrMapper.registerModule(AtrModule)

    val mapValue = atrMapper.readValue(jsonString, new TypeReference[Map[String,Any]] {})

    mapValue should contain key "sKey"
    mapValue("sKey") should be ("sValue")
    val mKeyValue = mapValue("mKey")
    mKeyValue shouldBe a [collection.immutable.TreeMap[_,_]]
    val typedMKeyValue = mKeyValue.asInstanceOf[collection.immutable.TreeMap[String,Any]]
    typedMKeyValue should contain key "mData"
    typedMKeyValue("mData") shouldBe "mValue"
  }
}
