package tools.jackson.module.scala.deser

import tools.jackson.databind.JacksonModule.SetupContext
import tools.jackson.databind.`type`.CollectionLikeType
import tools.jackson.databind.jsontype.TypeDeserializer
import tools.jackson.databind.{BeanDescription, DeserializationConfig, JavaType, ValueDeserializer}
import tools.jackson.module.scala.JacksonModule.InitializerBuilder
import tools.jackson.module.scala.ScalaModule
import tools.jackson.module.scala.modifiers.ScalaTypeModifierModule

import scala.collection._
import scala.reflect.ClassTag

trait SeqDeserializerModule extends ScalaTypeModifierModule {
  override def getInitializers(config: ScalaModule.Config): scala.Seq[SetupContext => Unit] = {
    super.getInitializers(config) ++ {
      val builder = new InitializerBuilder()
      builder += new GenericFactoryDeserializerResolver[Iterable, IterableFactory](config) {
        override val CLASS_DOMAIN: Class[Collection[_]] = classOf[Iterable[_]]
        private val BASE_CLASS_DOMAIN: Class[_] = classOf[Seq[_]]
        private val IGNORE_CLASS_DOMAIN: Class[_] = classOf[Set[_]]

        override val factories: Iterable[(Class[_], Factory)] = sortFactories(Vector(
          (classOf[IndexedSeq[_]], IndexedSeq),
          (classOf[Iterable[_]], Iterable),
          (classOf[Seq[_]], Seq),
          (classOf[LinearSeq[_]], LinearSeq),
          (classOf[immutable.Iterable[_]], immutable.Iterable),
          (classOf[immutable.IndexedSeq[_]], immutable.IndexedSeq),
          (classOf[immutable.LazyList[_]], immutable.LazyList),
          (classOf[immutable.LinearSeq[_]], immutable.LinearSeq),
          (classOf[immutable.List[_]], immutable.List),
          (classOf[immutable.Queue[_]], immutable.Queue),
          (classOf[immutable.Stream[_]], immutable.Stream),
          (classOf[immutable.Seq[_]], immutable.Seq),
          (classOf[immutable.Vector[_]], immutable.Vector),
          (classOf[mutable.ArrayBuffer[_]], mutable.ArrayBuffer),
          (classOf[mutable.ArrayDeque[_]], mutable.ArrayDeque),
          (classOf[mutable.Buffer[_]], mutable.Buffer),
          (classOf[mutable.IndexedSeq[_]], mutable.IndexedSeq),
          (classOf[mutable.Iterable[_]], mutable.Iterable),
          (classOf[mutable.ListBuffer[_]], mutable.ListBuffer),
          (classOf[mutable.Queue[_]], mutable.Queue),
          (classOf[mutable.Seq[_]], mutable.Seq),
          (classOf[mutable.Stack[_]], mutable.Stack)
        ))

        override def builderFor[A](cf: Factory, valueType: JavaType): Builder[A] = cf.newBuilder[A]

        // A few types need class tags and therefore do not use IterableFactory.
        type TagFactory = ClassTagIterableFactory[Collection]

        val tagFactories: Iterable[(Class[_], TagFactory)] = Seq(
          (classOf[mutable.ArraySeq[_]], mutable.ArraySeq),
          (classOf[immutable.ArraySeq[_]], immutable.ArraySeq),
          (classOf[mutable.UnrolledBuffer[_]], mutable.UnrolledBuffer)
        )

        def builderFor[A](cf: TagFactory, valueType: JavaType): Builder[A] =
          cf.newBuilder[A](ClassTag(valueType.getRawClass))

        def tryTagFactory[A](cls: Class[_], valueType: JavaType): Option[Builder[A]] = tagFactories
          .find(_._1.isAssignableFrom(cls))
          .map(_._2)
          .map(builderFor[A](_, valueType))

        override def builderFor[A](cls: Class[_], valueType: JavaType): Builder[A] = tryTagFactory[A](cls, valueType)
          .getOrElse(super.builderFor[A](cls, valueType))

        override def findCollectionLikeDeserializer(collectionType: CollectionLikeType,
                                                    deserializationConfig: DeserializationConfig,
                                                    beanDesc: BeanDescription,
                                                    elementTypeDeserializer: TypeDeserializer,
                                                    elementDeserializer: ValueDeserializer[_]): ValueDeserializer[_] = {
          val rawClass = collectionType.getRawClass
          if (IGNORE_CLASS_DOMAIN.isAssignableFrom(rawClass)) {
            None.orNull
          } else {
            super.findCollectionLikeDeserializer(collectionType,
              deserializationConfig, beanDesc, elementTypeDeserializer, elementDeserializer)
          }
        }

        override def hasDeserializerFor(deserializationConfig: DeserializationConfig, valueType: Class[_]): Boolean = {
          BASE_CLASS_DOMAIN.isAssignableFrom(valueType)
        }
      }
      builder.build()
    }
  }
}

object SeqDeserializerModule extends SeqDeserializerModule
