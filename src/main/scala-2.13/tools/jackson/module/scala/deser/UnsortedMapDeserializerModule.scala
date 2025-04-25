package tools.jackson.module.scala.deser

import tools.jackson.databind.JacksonModule.SetupContext
import tools.jackson.databind._
import tools.jackson.databind.`type`.MapLikeType
import tools.jackson.databind.jsontype.TypeDeserializer
import tools.jackson.module.scala.JacksonModule.InitializerBuilder
import tools.jackson.module.scala.ScalaModule
import tools.jackson.module.scala.modifiers.MapTypeModifierModule

import scala.collection._
import scala.language.existentials

trait UnsortedMapDeserializerModule extends MapTypeModifierModule {
  override def getInitializers(config: ScalaModule.Config): scala.Seq[SetupContext => Unit] = {
    super.getInitializers(config) ++ {
      val builder = new InitializerBuilder()
      builder += new GenericMapFactoryDeserializerResolver[Map, MapFactory](config) {

        override val CLASS_DOMAIN: Class[Collection[_, _]] = classOf[Map[_, _]]

        // OpenHashMap is omitted due to deprecation
        // WeakHashMap is omitted due to the unlikely use case
        override val factories: scala.Seq[(Class[_], Factory)] = sortFactories(Vector(
          (classOf[Map[_, _]], Map),
          (classOf[immutable.HashMap[_, _]], immutable.HashMap),
          (classOf[immutable.ListMap[_, _]], immutable.ListMap),
          (classOf[immutable.Map[_, _]], immutable.Map),
          (classOf[mutable.HashMap[_, _]], mutable.HashMap),
          (classOf[mutable.LinkedHashMap[_, _]], mutable.LinkedHashMap),
          (classOf[mutable.ListMap[_, _]], mutable.ListMap),
          (classOf[mutable.Map[_, _]], mutable.Map),
          (classOf[immutable.TreeSeqMap[_, _]], immutable.TreeSeqMap),
          (classOf[concurrent.TrieMap[_, _]], concurrent.TrieMap)
        ))

        override def builderFor[K, V](factory: Factory, keyType: JavaType, valueType: JavaType): Builder[K, V] = factory.newBuilder[K, V]

        override def findMapLikeDeserializer(theType: MapLikeType,
                                             config: DeserializationConfig,
                                             beanDesc: BeanDescription.Supplier,
                                             keyDeserializer: KeyDeserializer,
                                             elementTypeDeserializer: TypeDeserializer,
                                             elementDeserializer: ValueDeserializer[_]): ValueDeserializer[_] = {

          var deserializer = LongMapDeserializerResolver.findMapLikeDeserializer(
            theType, config, beanDesc, keyDeserializer, elementTypeDeserializer, elementDeserializer)
          if (deserializer == null) {
            deserializer = IntMapDeserializerResolver.findMapLikeDeserializer(
              theType, config, beanDesc, keyDeserializer, elementTypeDeserializer, elementDeserializer)
            if (deserializer == null) {
              deserializer = super.findMapLikeDeserializer(
                theType, config, beanDesc, keyDeserializer, elementTypeDeserializer, elementDeserializer)
            }
          }
          deserializer
        }
      }
      builder.build()
    }
  }
}

object UnsortedMapDeserializerModule extends UnsortedMapDeserializerModule

