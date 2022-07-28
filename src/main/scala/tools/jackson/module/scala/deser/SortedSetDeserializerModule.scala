package tools.jackson.module.scala.deser

import tools.jackson.databind.JacksonModule.SetupContext
import tools.jackson.databind.{DeserializationConfig, JavaType}
import tools.jackson.module.scala.JacksonModule.InitializerBuilder
import tools.jackson.module.scala.ScalaModule
import tools.jackson.module.scala.introspect.OrderingLocator
import tools.jackson.module.scala.modifiers.ScalaTypeModifierModule

import scala.collection._

trait SortedSetDeserializerModule extends ScalaTypeModifierModule {

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
      }
      builder.build()
    }
  }
}

object SortedSetDeserializerModule extends SortedSetDeserializerModule
