package tools.jackson.module.scala.introspect

import tools.jackson.databind.util.LookupCache
import tools.jackson.module.scala.{DefaultLookupCacheFactory, LookupCacheFactory, ScalaTypeInfo}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

case class DerivedFields(aLong: Option[Long], aStr: Option[String]) derives ScalaTypeInfo

case class PlainFields(aLong: Option[Long])

class DerivedTypeInfoSpec extends AnyWordSpec with Matchers {

  private def derivedTypeInfo = new DerivedTypeInfo(DefaultLookupCacheFactory)

  "DerivedTypeInfo" should {
    "read the table a class captured" in {
      derivedTypeInfo.erasedTypeArguments(classOf[DerivedFields]).toMap shouldEqual
        Map("aLong" -> classOf[Long])
    }
    "answer with an empty table for a class that derived nothing" in {
      derivedTypeInfo.erasedTypeArguments(classOf[PlainFields]) shouldBe empty
    }
    // reading the table builds a fresh Seq every time, so the same instance coming back is the memo
    // answering rather than the companion being read again
    "read the table once and remember it" in {
      val info = derivedTypeInfo
      val first = info.erasedTypeArguments(classOf[DerivedFields])
      first should not be empty
      info.erasedTypeArguments(classOf[DerivedFields]) should be theSameInstanceAs first
    }
    "keep what it remembered to itself" in {
      val first = derivedTypeInfo.erasedTypeArguments(classOf[DerivedFields])
      derivedTypeInfo.erasedTypeArguments(classOf[DerivedFields]) should not be
        theSameInstanceAs(first)
    }
    "build its cache with the factory it was given" in {
      var created = 0
      val factory = new LookupCacheFactory {
        override def createLookupCache[K, V](initialEntries: Int, maxEntries: Int): LookupCache[K, V] = {
          created += 1
          DefaultLookupCacheFactory.createLookupCache(initialEntries, maxEntries)
        }
      }
      new DerivedTypeInfo(factory)
      created shouldEqual 1
    }
  }
}
