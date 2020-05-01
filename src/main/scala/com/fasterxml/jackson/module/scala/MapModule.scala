package __foursquare_shaded__.com.fasterxml.jackson.module.scala

import __foursquare_shaded__.com.fasterxml.jackson.module.scala.deser.{SortedMapDeserializerModule, UnsortedMapDeserializerModule}
import __foursquare_shaded__.com.fasterxml.jackson.module.scala.ser.MapSerializerModule

trait MapModule
  extends MapSerializerModule
    with UnsortedMapDeserializerModule
    with SortedMapDeserializerModule
