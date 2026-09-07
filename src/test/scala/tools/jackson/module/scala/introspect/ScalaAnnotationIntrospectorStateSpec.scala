package tools.jackson.module.scala.introspect

import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.scala.{DefaultLookupCacheFactory, ScalaModule}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.util.concurrent.{CountDownLatch, Executors, TimeUnit}

case class IntrospectorStateHolder(valueLong: Option[Long])

/**
 * The introspector's caches and its registered referenced value types belong to a module instance,
 * so a mapper built from its own ScalaModule does not share them with every other mapper.
 */
class ScalaAnnotationIntrospectorStateSpec extends AnyWordSpec with Matchers {

  "ScalaAnnotationIntrospectorModule" should {
    "give each builder its own introspector instance" in {
      val first = ScalaModule.builder()
      val second = ScalaModule.builder()
      first.scalaAnnotationIntrospectorModule should not be
        theSameInstanceAs(second.scalaAnnotationIntrospectorModule)
      first.scalaAnnotationIntrospectorModule should not be
        theSameInstanceAs(ScalaAnnotationIntrospectorModule)
    }
    "give each builder its own record of what classes derived" in {
      val first = ScalaModule.builder()
      val second = ScalaModule.builder()
      first.scalaAnnotationIntrospectorModule._derivedTypeInfo should not be
        theSameInstanceAs(second.scalaAnnotationIntrospectorModule._derivedTypeInfo)
      first.scalaAnnotationIntrospectorModule._derivedTypeInfo should not be
        theSameInstanceAs(ScalaAnnotationIntrospectorModule._derivedTypeInfo)
    }
    "rebuild that record when the lookup cache factory is replaced" in {
      val module = ScalaAnnotationIntrospectorModule.newStandaloneInstance()
      val before = module._derivedTypeInfo
      module.setLookupCacheFactory(DefaultLookupCacheFactory)
      module._derivedTypeInfo should not be theSameInstanceAs(before)
    }
    "keep a referenced value type registration to the builder it was made on" in {
      val builder = ScalaModule.builder().addAllBuiltinModules()
      try {
        builder.scalaAnnotationIntrospectorModule
          .registerReferencedValueType(classOf[IntrospectorStateHolder], "valueLong", classOf[Long])
        builder.scalaAnnotationIntrospectorModule
          .getRegisteredReferencedValueType(classOf[IntrospectorStateHolder], "valueLong") shouldEqual Some(classOf[Long])
        // the module object knows nothing of it
        ScalaAnnotationIntrospectorModule
          .getRegisteredReferencedValueType(classOf[IntrospectorStateHolder], "valueLong") shouldBe empty
      } finally {
        builder.scalaAnnotationIntrospectorModule.clearRegisteredReferencedTypes()
      }
    }
    // introspecting a class registers what it derived, on whatever thread first deserializes it, so
    // registrations are made concurrently now rather than once at startup from one thread
    "keep every registration made from many threads at once" in {
      val module = ScalaAnnotationIntrospectorModule.newStandaloneInstance()
      val threads = 8
      val perThread = 250
      val pool = Executors.newFixedThreadPool(threads)
      val start = new CountDownLatch(1)
      try {
        (0 until threads).foreach { t =>
          pool.execute(new Runnable {
            override def run(): Unit = {
              start.await()
              (0 until perThread).foreach { i =>
                module.registerReferencedValueType(classOf[IntrospectorStateHolder], s"field-$t-$i", classOf[Long])
              }
            }
          })
        }
        start.countDown()
        pool.shutdown()
        pool.awaitTermination(60, TimeUnit.SECONDS) shouldBe true
        val missing = for {
          t <- 0 until threads
          i <- 0 until perThread
          if module.getRegisteredReferencedValueType(classOf[IntrospectorStateHolder], s"field-$t-$i").isEmpty
        } yield s"field-$t-$i"
        withClue(s"${missing.size} of ${threads * perThread} registrations were lost: ") {
          missing shouldBe empty
        }
      } finally {
        pool.shutdownNow()
        module.clearRegisteredReferencedTypes()
      }
    }

    "keep registrations for different classes made from many threads at once" in {
      val module = ScalaAnnotationIntrospectorModule.newStandaloneInstance()
      val classes = Seq(classOf[IntrospectorStateHolder], classOf[String], classOf[Integer], classOf[java.lang.Long])
      val threads = 8
      val pool = Executors.newFixedThreadPool(threads)
      val start = new CountDownLatch(1)
      try {
        (0 until threads).foreach { t =>
          pool.execute(new Runnable {
            override def run(): Unit = {
              start.await()
              (0 until 250).foreach { i =>
                classes.foreach(c => module.registerReferencedValueType(c, s"field-$t-$i", classOf[Long]))
              }
            }
          })
        }
        start.countDown()
        pool.shutdown()
        pool.awaitTermination(60, TimeUnit.SECONDS) shouldBe true
        val missing = for {
          c <- classes
          t <- 0 until threads
          i <- 0 until 250
          if module.getRegisteredReferencedValueType(c, s"field-$t-$i").isEmpty
        } yield s"${c.getName}.field-$t-$i"
        withClue(s"${missing.size} registrations were lost: ") {
          missing shouldBe empty
        }
      } finally {
        pool.shutdownNow()
        module.clearRegisteredReferencedTypes()
      }
    }

    "still recognise the module object when removing it from a builder" in {
      val builder = ScalaModule.builder().addAllBuiltinModules()
      builder.hasModule(ScalaAnnotationIntrospectorModule) shouldEqual true
      builder.removeModule(ScalaAnnotationIntrospectorModule)
      builder.hasModule(ScalaAnnotationIntrospectorModule) shouldEqual false
    }
    "read a case class through a module built by the builder" in {
      val mapper = JsonMapper.builder().addModule(ScalaModule.builder().addAllBuiltinModules().build()).build()
      val value = IntrospectorStateHolder(Some(3L))
      mapper.readValue(mapper.writeValueAsString(value), classOf[IntrospectorStateHolder]) shouldEqual value
    }
  }
}
