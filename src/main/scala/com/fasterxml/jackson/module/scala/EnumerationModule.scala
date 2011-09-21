package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.module.scala.deser.EnumerationDeserializerModule
import com.fasterxml.jackson.module.scala.ser.EnumerationSerializerModule

/**
 * @author Christopher Currie <ccurrie@impresys.com>
 */
trait EnumerationModule extends EnumerationSerializerModule with EnumerationDeserializerModule {

}