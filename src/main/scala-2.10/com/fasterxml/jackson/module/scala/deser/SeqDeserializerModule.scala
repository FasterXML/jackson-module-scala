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
      .add(immutable.Iterable)
      .add(immutable.IndexedSeq)
      .add(immutable.List)
      .add(immutable.Queue)
      .add(immutable.Stream)
      .add(immutable.Seq)
      .add(immutable.Vector)
      .add(mutable.ArrayBuffer)
      .add(mutable.ArraySeq)
      .add(mutable.Buffer)
      .add(mutable.IndexedSeq)
      .add(mutable.Iterable)
      .add(mutable.LinearSeq)
      .add(mutable.ListBuffer)
      .add(mutable.MutableList)
      .add(mutable.Queue)
      .add(mutable.ResizableArray)
      .add(mutable.Seq)
      .add(mutable.Stack)
      .toList

    override def builderFor[A](cf: Factory, valueType: JavaType): Builder[A] = cf.newBuilder[A]

    // UnrolledBuffer is in a class of its own pre 2.13...
    override def builderFor[A](cls: Class[_], valueType: JavaType): Builder[A] = {
      if (classOf[mutable.UnrolledBuffer[_]].isAssignableFrom(cls)) {
        mutable.UnrolledBuffer.newBuilder[A](ClassTag(valueType.getRawClass))
      } else {
        super.builderFor[A](cls, valueType)
      }
    }
  })
}
