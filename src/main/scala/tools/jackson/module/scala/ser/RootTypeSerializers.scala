package tools.jackson.module.scala.ser

import tools.jackson.core.JsonGenerator
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.jsontype.TypeSerializer

import java.util.concurrent.ConcurrentHashMap
import java.util.function.{Function => JFunction}
import scala.collection.{immutable, mutable}

/**
 * A root value has no declared type, so databind decides whether to write a type id from its runtime
 * class. Scala's collection implementations are nearly all final (Map$Map2, $colon$colon, Vector...),
 * so under NON_FINAL default typing a root Scala collection is written without the type id that
 * reading it back as Map/Seq/Set then demands. Serializers call this at root level to be typed the
 * way the collection's abstract type would be, which is what writerFor(classOf[Map[_, _]]) does.
 *
 * Resolving a type serializer walks the whole supertype tree of the abstract type once per call, so
 * each serializer keeps what it resolved: a type serializer holds no per-call state, and default typing
 * cannot change once a mapper is built.
 */
private[ser] final class RootTypeSerializers(abstractTypeOf: AnyRef => Class[_]) {

  // the value's abstract class -> the type serializer found for it, or None once found to be absent
  private val resolved = new ConcurrentHashMap[Class[_], Option[TypeSerializer]]()

  def rootTypeSerializer(gen: JsonGenerator, ctxt: SerializationContext, value: AnyRef): TypeSerializer = {
    if (!gen.streamWriteContext().inRoot()) None.orNull
    else {
      val resolve: JFunction[Class[_], Option[TypeSerializer]] =
        cls => Option(ctxt.findTypeSerializer(ctxt.constructType(cls)))
      resolved.computeIfAbsent(abstractTypeOf(value), resolve).orNull
    }
  }
}

private[ser] object RootTypeSerializers {

  def forMaps(): RootTypeSerializers = new RootTypeSerializers({
    case _: immutable.Map[_, _] => classOf[immutable.Map[_, _]]
    case _: mutable.Map[_, _] => classOf[mutable.Map[_, _]]
    case _ => classOf[collection.Map[_, _]]
  })

  def forIterables(): RootTypeSerializers = new RootTypeSerializers({
    case _: immutable.Iterable[_] => classOf[immutable.Iterable[_]]
    case _: mutable.Iterable[_] => classOf[mutable.Iterable[_]]
    case _ => classOf[collection.Iterable[_]]
  })
}
