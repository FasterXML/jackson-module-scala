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
        private val UNROLLED_BUFFER_CLASS: Class[_] = classOf[mutable.UnrolledBuffer[_]]

        override val factories: Iterable[(Class[_], Factory)] = sortFactories(Vector(
          (classOf[IndexedSeq[_]], IndexedSeq.asInstanceOf[Factory]),
          (classOf[Iterable[_]], Iterable.asInstanceOf[Factory]),
          (classOf[Seq[_]], Seq.asInstanceOf[Factory]),
          (classOf[immutable.Iterable[_]], immutable.Iterable.asInstanceOf[Factory]),
          (classOf[immutable.IndexedSeq[_]], immutable.IndexedSeq.asInstanceOf[Factory]),
          (classOf[immutable.List[_]], immutable.List.asInstanceOf[Factory]),
          (classOf[immutable.Queue[_]], immutable.Queue.asInstanceOf[Factory]),
          (classOf[immutable.Stream[_]], immutable.Stream.asInstanceOf[Factory]),
          (classOf[immutable.Seq[_]], immutable.Seq.asInstanceOf[Factory]),
          (classOf[immutable.Vector[_]], immutable.Vector.asInstanceOf[Factory]),
          (classOf[mutable.ArrayBuffer[_]], mutable.ArrayBuffer.asInstanceOf[Factory]),
          (classOf[mutable.ArraySeq[_]], mutable.ArraySeq.asInstanceOf[Factory]),
          (classOf[mutable.Buffer[_]], mutable.Buffer.asInstanceOf[Factory]),
          (classOf[mutable.IndexedSeq[_]], mutable.IndexedSeq.asInstanceOf[Factory]),
          (classOf[mutable.Iterable[_]], mutable.Iterable.asInstanceOf[Factory]),
          (classOf[mutable.LinearSeq[_]], mutable.LinearSeq.asInstanceOf[Factory]),
          (classOf[mutable.ListBuffer[_]], mutable.ListBuffer.asInstanceOf[Factory]),
          (classOf[mutable.MutableList[_]], mutable.MutableList.asInstanceOf[Factory]),
          (classOf[mutable.Queue[_]], mutable.Queue.asInstanceOf[Factory]),
          (classOf[mutable.ResizableArray[_]], mutable.ResizableArray.asInstanceOf[Factory]),
          (classOf[mutable.Seq[_]], mutable.Seq.asInstanceOf[Factory]),
          (classOf[mutable.Stack[_]], mutable.Stack.asInstanceOf[Factory])
        ))

        override def builderFor[A](cf: Factory, valueType: JavaType): Builder[A] = cf.newBuilder[A]

        // UnrolledBuffer is in a class of its own pre 2.13...
        override def builderFor[A](cls: Class[_], valueType: JavaType): Builder[A] = {
          if (UNROLLED_BUFFER_CLASS.isAssignableFrom(cls)) {
            mutable.UnrolledBuffer.newBuilder[A](ClassTag(valueType.getRawClass))
          } else {
            super.builderFor[A](cls, valueType)
          }
        }

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
