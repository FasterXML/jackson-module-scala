package tools.jackson.module.scala.deser

import tools.jackson.databind.JacksonModule.SetupContext
import tools.jackson.databind.`type`.CollectionLikeType
import tools.jackson.databind.jsontype.TypeDeserializer
import tools.jackson.databind.{BeanDescription, DeserializationConfig, JavaType, ValueDeserializer}
import tools.jackson.module.scala.JacksonModule.InitializerBuilder
import tools.jackson.module.scala.ScalaModule
import tools.jackson.module.scala.introspect.OrderingLocator
import tools.jackson.module.scala.modifiers.ScalaTypeModifierModule

import scala.collection._

trait SortedSetDeserializerModule extends ScalaTypeModifierModule {

  private val IMMUTABLE_BITSET_CLASS: Class[_] = classOf[immutable.BitSet]
  private val MUTABLE_BITSET_CLASS: Class[_] = classOf[mutable.BitSet]

  override def getInitializers(config: ScalaModule.Config): scala.Seq[SetupContext => Unit] = {
    super.getInitializers(config) ++ {
      val builder = new InitializerBuilder()
      builder += new GenericFactoryDeserializerResolver[SortedSet, SortedIterableFactory](config) {

        override val CLASS_DOMAIN: Class[Collection[_]] = classOf[SortedSet[_]]

        override val factories: Iterable[(Class[_], Factory)] = sortFactories(Vector(
          (classOf[SortedSet[_]], SortedSet.asInstanceOf[Factory]),
          (classOf[immutable.TreeSet[_]], immutable.TreeSet.asInstanceOf[Factory]),
          (classOf[immutable.SortedSet[_]], immutable.SortedSet.asInstanceOf[Factory]),
          (classOf[mutable.TreeSet[_]], mutable.TreeSet.asInstanceOf[Factory]),
          (classOf[mutable.SortedSet[_]], mutable.SortedSet.asInstanceOf[Factory])
        ))

        override def builderFor[A](cf: Factory, valueType: JavaType): Builder[A] =
          cf.newBuilder[A](OrderingLocator.locate(valueType).asInstanceOf[Ordering[A]])

        override def hasDeserializerFor(deserializationConfig: DeserializationConfig, valueType: Class[_]): Boolean = {
          // TODO add implementation
          false
        }

        override def findCollectionLikeDeserializer(collectionType: CollectionLikeType,
                                                    config: DeserializationConfig,
                                                    beanDesc: BeanDescription,
                                                    elementTypeDeserializer: TypeDeserializer,
                                                    elementDeserializer: ValueDeserializer[_]): ValueDeserializer[_] = {
          val rawClass = collectionType.getRawClass
          if (IMMUTABLE_BITSET_CLASS.isAssignableFrom(rawClass)) {
            ImmutableBitSetDeserializer
          } else if (MUTABLE_BITSET_CLASS.isAssignableFrom(rawClass)) {
            MutableBitSetDeserializer
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

object SortedSetDeserializerModule extends SortedSetDeserializerModule
