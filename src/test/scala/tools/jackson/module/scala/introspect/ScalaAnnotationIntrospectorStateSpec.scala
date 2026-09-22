package tools.jackson.module.scala.introspect

import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.util.LookupCache
import tools.jackson.module.scala.{DefaultLookupCacheFactory, ScalaModule}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.util.concurrent.{CountDownLatch, Executors, TimeUnit}

case class IntrospectorStateHolder(valueLong: Option[Long])

case class DescriptorRaceHolder(value: Int)

object ScalaAnnotationIntrospectorStateSpec {

  /**
   * A descriptor cache as the thread that loses a race sees it: nothing is there when it looks, and
   * by the time it stores what it built another thread has already stored its own, which is what
   * `putIfAbsent` hands back.
   */
  class LostRaceCache[K, V](winners: LookupCache[K, V]) extends LookupCache[K, V] {
    override def get(key: K): V = None.orNull.asInstanceOf[V]
    override def putIfAbsent(key: K, value: V): V = winners.get(key)
    override def put(key: K, value: V): V = winners.put(key, value)
    override def clear(): Unit = winners.clear()
    override def size: Int = winners.size
    override def snapshot(): LookupCache[K, V] = ???
    override def emptyCopy(): LookupCache[K, V] = ???
  }
}

/**
 * The introspector's caches and its registered referenced value types belong to a module instance,
 * so a mapper built from its own ScalaModule does not share them with every other mapper.
 */
class ScalaAnnotationIntrospectorStateSpec extends AnyWordSpec with Matchers {

  import ScalaAnnotationIntrospectorStateSpec._

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
    // two threads meeting a class for the first time both introspect it; only one of the two
    // descriptors they build reaches the cache, and both threads have to go on with that one
    "describe a class with the descriptor that won the race to the cache" in {
      val builder = ScalaModule.builder().addAllBuiltinModules()
      val module = builder.scalaAnnotationIntrospectorModule
      val key = classOf[DescriptorRaceHolder].getName
      // what the thread that got there first stored: the same class, described under another name
      val winner = {
        val built = BeanIntrospector(classOf[DescriptorRaceHolder])
        built.copy(properties = built.properties.map(_.copy(name = "won")))
      }
      val winners = DefaultLookupCacheFactory.createLookupCache[String, BeanDescriptor](16, 100)
      winners.put(key, winner)
      module._descriptorCache = new LostRaceCache(winners)

      val mapper = JsonMapper.builder().addModule(builder.build()).build()
      mapper.writeValueAsString(DescriptorRaceHolder(1)) shouldEqual """{"won":1}"""
    }

    "read a case class through a module built by the builder" in {
      val mapper = JsonMapper.builder().addModule(ScalaModule.builder().addAllBuiltinModules().build()).build()
      val value = IntrospectorStateHolder(Some(3L))
      mapper.readValue(mapper.writeValueAsString(value), classOf[IntrospectorStateHolder]) shouldEqual value
    }
  }
}
