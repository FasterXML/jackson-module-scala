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

case class Made private (aLong: Option[Long]) derives ScalaTypeInfo
object Made {
  @com.fasterxml.jackson.annotation.JsonCreator
  def make(aLong: Option[Long], plain: String): Made = Made(aLong)
}

// a hierarchy two deep, marked at the top, with an unrelated derived companion in between
sealed trait Outer derives ScalaTypeInfo
sealed trait Inner extends Outer
case class Leaf(aLong: Option[Long]) extends Inner

// mix-ins for a class that derived nothing: one extends it, one repeats its member
trait ExtendingMixin extends PlainFields derives ScalaTypeInfo
case class RepeatingMixin(aLong: Option[Int]) derives ScalaTypeInfo

class DerivedTypeInfoSpec extends AnyWordSpec with Matchers {

  private def derivedTypeInfo = new DerivedTypeInfo(DefaultLookupCacheFactory)

  "DerivedTypeInfo" should {
    "read the table a class captured" in {
      derivedTypeInfo.erasedFields(classOf[DerivedFields], None).toMap shouldEqual
        Map("aLong" -> DerivedTypeShape(classOf[Option[?]], Seq(DerivedTypeShape(classOf[Long], Seq.empty))))
    }
    "read the table a class's sealed base captured for it" in {
      derivedTypeInfo.erasedFields(classOf[Marked], None).map(_._1) shouldEqual Seq("aLong")
      derivedTypeInfo.erasedFields(classOf[Leaf], None).map(_._1) shouldEqual Seq("aLong")
      // the base describes only its own implementations
      derivedTypeInfo.erasedFields(classOf[Unmarked], None) shouldBe empty
    }
    "read the creator parameters a class captured" in {
      derivedTypeInfo.erasedCreatorParameters(classOf[Made], None) shouldEqual Seq(
        DerivedCreatorParameter("make", 2, 0) -> DerivedTypeShape(classOf[Option[?]], Seq(DerivedTypeShape(classOf[Long], Seq.empty))))
      derivedTypeInfo.erasedCreatorParameters(classOf[DerivedFields], None) shouldBe empty
    }
    "answer with an empty table for a class that derived nothing" in {
      derivedTypeInfo.erasedFields(classOf[PlainFields], None) shouldBe empty
    }
    "read the table a mix-in captured for a class, whether keyed by the class or by the mix-in" in {
      val long = DerivedTypeShape(classOf[Option[?]], Seq(DerivedTypeShape(classOf[Long], Seq.empty)))
      val int = DerivedTypeShape(classOf[Option[?]], Seq(DerivedTypeShape(classOf[Int], Seq.empty)))
      derivedTypeInfo.erasedFields(classOf[PlainFields], Some(classOf[ExtendingMixin])) shouldEqual Seq("aLong" -> long)
      derivedTypeInfo.erasedFields(classOf[PlainFields], Some(classOf[RepeatingMixin])) shouldEqual Seq("aLong" -> int)
    }
    "put what a mix-in captured ahead of what the class captured" in {
      val int = DerivedTypeShape(classOf[Option[?]], Seq(DerivedTypeShape(classOf[Int], Seq.empty)))
      derivedTypeInfo.erasedFields(classOf[DerivedFields], Some(classOf[RepeatingMixin])).map(_._1) shouldEqual Seq("aLong", "aLong")
      derivedTypeInfo.erasedFields(classOf[DerivedFields], Some(classOf[RepeatingMixin])).head shouldEqual ("aLong" -> int)
    }
    "read nothing from a mix-in that derived nothing" in {
      derivedTypeInfo.erasedFields(classOf[DerivedFields], Some(classOf[Unmarked])).map(_._1) shouldEqual Seq("aLong")
    }
    // reading the table builds a fresh Seq every time, so the same instance coming back is the memo
    // answering rather than the companion being read again
    "read the table once and remember it" in {
      val info = derivedTypeInfo
      val first = info.erasedFields(classOf[DerivedFields], None)
      first should not be empty
      info.erasedFields(classOf[DerivedFields], None) should be theSameInstanceAs first
    }
    "keep what it remembered to itself" in {
      val first = derivedTypeInfo.erasedFields(classOf[DerivedFields], None)
      derivedTypeInfo.erasedFields(classOf[DerivedFields], None) should not be
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
