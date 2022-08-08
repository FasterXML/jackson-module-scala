package tools.jackson.module.scala.deser

import tools.jackson.core.`type`.TypeReference
import tools.jackson.databind.JacksonModule.SetupContext
import tools.jackson.databind.`type`.MapLikeType
import tools.jackson.databind.{AbstractTypeResolver, DeserializationConfig, JavaType}
import tools.jackson.module.scala.{DefaultScalaModule, JacksonModule, ScalaModule}

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
      override def getInitializers(config: ScalaModule.Config): Seq[SetupContext => Unit] = {
        val builder = new JacksonModule.InitializerBuilder()
        builder += (_ addAbstractTypeResolver ATR)
        builder.build()
      }
    }

    val atrMapper = newBuilder.addModule(AtrModule).build()
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