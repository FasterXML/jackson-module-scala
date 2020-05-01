package __foursquare_shaded__.com.fasterxml.jackson.module.scala

import __foursquare_shaded__.com.fasterxml.jackson.module.scala.deser.OptionDeserializerModule
import __foursquare_shaded__.com.fasterxml.jackson.module.scala.ser.OptionSerializerModule

/**
 * Adds support for serializing and deserializing Scala Options.
 */
trait OptionModule extends OptionSerializerModule with OptionDeserializerModule
