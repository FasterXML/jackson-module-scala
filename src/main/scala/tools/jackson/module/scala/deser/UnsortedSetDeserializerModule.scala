package tools.jackson.module.scala.deser

import tools.jackson.databind.JacksonModule.SetupContext
import tools.jackson.databind.`type`.CollectionLikeType
import tools.jackson.databind.jsontype.TypeDeserializer
import tools.jackson.databind.{BeanDescription, DeserializationConfig, JavaType, ValueDeserializer}
import tools.jackson.module.scala.JacksonModule.InitializerBuilder
import tools.jackson.module.scala.ScalaModule
import tools.jackson.module.scala.modifiers.ScalaTypeModifierModule

import scala.collection._

trait UnsortedSetDeserializerModule extends ScalaTypeModifierModule {
  override def getModuleName: String = "UnsortedSetDeserializerModule"

  override def getInitializers(config: ScalaModule.Config): scala.Seq[SetupContext => Unit] = {
    super.getInitializers(config) ++ {
      val builder = new InitializerBuilder()
      builder += new GenericFactoryDeserializerResolver[Set, IterableFactory](config) {

        override val CLASS_DOMAIN: Class[Collection[_]] = classOf[Set[_]]
        private val IGNORE_CLASS_DOMAIN: Class[_] = classOf[SortedSet[_]]

        override val factories: Iterable[(Class[_], Factory)] = sortFactories(Vector(
          (classOf[Set[_]], Set.asInstanceOf[Factory]),
          (classOf[immutable.HashSet[_]], immutable.HashSet.asInstanceOf[Factory]),
          (classOf[immutable.ListSet[_]], immutable.ListSet.asInstanceOf[Factory]),
          (classOf[immutable.Set[_]], immutable.Set.asInstanceOf[Factory]),
          (classOf[mutable.HashSet[_]], mutable.HashSet.asInstanceOf[Factory]),
          (classOf[mutable.LinkedHashSet[_]], mutable.LinkedHashSet.asInstanceOf[Factory]),
          (classOf[mutable.Set[_]], mutable.Set.asInstanceOf[Factory])
        ))

        override def builderFor[A](cf: Factory, javaType: JavaType): Builder[A] = cf.newBuilder[A]

        override def hasDeserializerFor(deserializationConfig: DeserializationConfig, valueType: Class[_]): Boolean = {
          CLASS_DOMAIN.isAssignableFrom(valueType) && !IGNORE_CLASS_DOMAIN.isAssignableFrom(valueType)
        }


        override def findCollectionLikeDeserializer(collectionType: CollectionLikeType,
                                                    config: DeserializationConfig,
                                                    beanDesc: BeanDescription.Supplier,
                                                    elementTypeDeserializer: TypeDeserializer,
                                                    elementDeserializer: ValueDeserializer[_]): ValueDeserializer[_] = {
          val rawClass = collectionType.getRawClass
          if (IGNORE_CLASS_DOMAIN.isAssignableFrom(rawClass)) {
            None.orNull
          } else {
            super.findCollectionLikeDeserializer(collectionType,
              config, beanDesc, elementTypeDeserializer, elementDeserializer)
          }
        }
      }
      builder.build()
    }
  }
}

object UnsortedSetDeserializerModule extends UnsortedSetDeserializerModule