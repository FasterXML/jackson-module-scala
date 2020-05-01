package __foursquare_shaded__.com.fasterxml.jackson.module.scala.deser

import __foursquare_shaded__.com.fasterxml.jackson.databind.JavaType
import __foursquare_shaded__.com.fasterxml.jackson.module.scala.modifiers.ScalaTypeModifierModule
import __foursquare_shaded__.com.fasterxml.jackson.module.scala.util.FactorySorter

import scala.collection._
import scala.reflect.ClassTag

trait SeqDeserializerModule extends ScalaTypeModifierModule {
  this += (_ addDeserializers new GenericFactoryDeserializerResolver[Iterable, IterableFactory] {
    override val CLASS_DOMAIN: Class[Collection[_]] = classOf[Iterable[_]]

    override val factories: Iterable[(Class[_], Factory)] = new FactorySorter[Collection, IterableFactory]()
      .add(IndexedSeq)
      .add(Iterable)
      .add(Seq)
      .add(LinearSeq)
      .add(immutable.Iterable)
      .add(immutable.IndexedSeq)
      .add(immutable.LazyList)
      .add(immutable.LinearSeq)
      .add(immutable.List)
      .add(immutable.Queue)
      .add(immutable.Stream)
      .add(immutable.Seq)
      .add(immutable.Vector)
      .add(mutable.ArrayBuffer)
      .add(mutable.ArrayDeque)
      .add(mutable.Buffer)
      .add(mutable.IndexedSeq)
      .add(mutable.Iterable)
      .add(mutable.ListBuffer)
      .add(mutable.Queue)
      .add(mutable.Seq)
      .add(mutable.Stack)
      .toList

    override def builderFor[A](cf: Factory, valueType: JavaType): Builder[A] = cf.newBuilder[A]

    // A few types need class tags and therefore do not use IterableFactory.
    type TagFactory = ClassTagIterableFactory[Collection]

    val tagFactories: Iterable[(Class[_], TagFactory)] =
      new FactorySorter[Collection, ClassTagIterableFactory]()
        .add(mutable.ArraySeq)
        .add(mutable.UnrolledBuffer)
        .toList

    def builderFor[A](cf: TagFactory, valueType: JavaType): Builder[A] =
      cf.newBuilder[A](ClassTag(valueType.getRawClass))

    def tryTagFactory[A](cls: Class[_], valueType: JavaType): Option[Builder[A]] = tagFactories
      .find(_._1.isAssignableFrom(cls))
      .map(_._2)
      .map(builderFor[A](_, valueType))

    override def builderFor[A](cls: Class[_], valueType: JavaType): Builder[A] = tryTagFactory[A](cls, valueType)
        .getOrElse(super.builderFor[A](cls, valueType))
  })
}
