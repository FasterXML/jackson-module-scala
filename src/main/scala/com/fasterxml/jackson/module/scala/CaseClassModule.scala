package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.module.scala.ser.CaseClassSerializerModule
import com.fasterxml.jackson.module.scala.deser.CaseClassDeserializerModule

/**
 * Adds support for serializing and deserializing case classes.
 *
 * The serialization logic restricts support to derived classes of [[scala.Product]],
 * (except for [[scala.TupleN]], which are serialized as JSON arrays in
 * [[com.fasterxml.jackson.module.scala.TupleModule]]).
 *
 * The deserialization logic is not case class specific but has not been tested for
 * any other types.
 *
 * @author Christopher Currie <christopher@currie.com>
 */
trait CaseClassModule extends CaseClassSerializerModule with CaseClassDeserializerModule {

}