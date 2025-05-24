package tools.jackson.module.scala

import java.util.ServiceLoader

import scala.collection.JavaConverters._

class ServiceLoaderSpec extends BaseSpec {
  "A ServiceLoader" should "be able to load DefaultScalaModule" in {
    val loader = ServiceLoader.load(classOf[tools.jackson.databind.JacksonModule])
    val modules = loader.iterator().asScala.toList
    modules should not be empty
    modules.collectFirst {
      case m if m.getClass == classOf[DefaultScalaModule] => m
    } should not be empty
  }
}
