package tools.jackson.module.scala.introspect

import tools.jackson.databind.util.LookupCache
import tools.jackson.module.scala.{DefaultLookupCacheFactory, LookupCacheFactory, ScalaTypeInfo}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

case class DerivedFields(aLong: Option[Long], aStr: Option[String]) derives ScalaTypeInfo

case class PlainFields(aLong: Option[Long])

// the trace is on the base's companion, not on Marked's own
sealed trait MarkedBase derives ScalaTypeInfo
case class Marked(aLong: Option[Long]) extends MarkedBase
case class Unmarked(aLong: Option[Long])

// a hierarchy two deep, marked at the top, with an unrelated derived companion in between
sealed trait Outer derives ScalaTypeInfo
sealed trait Inner extends Outer
case class Leaf(aLong: Option[Long]) extends Inner

class DerivedTypeInfoSpec extends AnyWordSpec with Matchers {

  private def derivedTypeInfo = new DerivedTypeInfo(DefaultLookupCacheFactory)

  "DerivedTypeInfo" should {
    "read the table a class captured" in {
      derivedTypeInfo.erasedFields(classOf[DerivedFields]).toMap shouldEqual
        Map("aLong" -> DerivedTypeShape(classOf[Option[?]], Seq(DerivedTypeShape(classOf[Long], Seq.empty))))
    }
    "read the table a class's sealed base captured for it" in {
      derivedTypeInfo.erasedFields(classOf[Marked]).map(_._1) shouldEqual Seq("aLong")
      derivedTypeInfo.erasedFields(classOf[Leaf]).map(_._1) shouldEqual Seq("aLong")
      // the base describes only its own implementations
      derivedTypeInfo.erasedFields(classOf[Unmarked]) shouldBe empty
    }
    "answer with an empty table for a class that derived nothing" in {
      derivedTypeInfo.erasedFields(classOf[PlainFields]) shouldBe empty
    }
    // reading the table builds a fresh Seq every time, so the same instance coming back is the memo
    // answering rather than the companion being read again
    "read the table once and remember it" in {
      val info = derivedTypeInfo
      val first = info.erasedFields(classOf[DerivedFields])
      first should not be empty
      info.erasedFields(classOf[DerivedFields]) should be theSameInstanceAs first
    }
    "keep what it remembered to itself" in {
      val first = derivedTypeInfo.erasedFields(classOf[DerivedFields])
      derivedTypeInfo.erasedFields(classOf[DerivedFields]) should not be
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
