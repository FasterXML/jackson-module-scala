package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.module.scala.deser.EnumerationDeserializerModule
import com.fasterxml.jackson.module.scala.ser.EnumerationSerializerModule

/**
 * Adds serialization and deserization support for Scala Enumerations.
 *
 * @author Christopher Currie <ccurrie@impresys.com>
 */
trait EnumerationModule extends EnumerationSerializerModule with EnumerationDeserializerModule {

}