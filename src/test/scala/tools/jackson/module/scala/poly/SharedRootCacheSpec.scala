package tools.jackson.module.scala.poly

import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.scala.poly.sharedroot.{InnerMixin, TopMixin, first, second}
import tools.jackson.module.scala.{BaseSpec, DefaultScalaModule}

/**
 * Every mapper that registers `DefaultScalaModule` shares one `SealedPolymorphism` instance, and
 * with it one cache of resolved `@type` names. A name is resolved against the root of the marked
 * hierarchy, and a mix-in makes which type that is a property of the mapper - so the root has to be
 * part of the cache key, or whichever mapper resolves a name first answers for the other one too.
 */
class SharedRootCacheSpec extends BaseSpec {

  private def marking(base: Class[_], mixin: Class[_]): JsonMapper =
    JsonMapper.builder().addModule(DefaultScalaModule).addMixIn(base, mixin).build()

  private val taggedShared = """{"@type":"Shared","v":"x"}"""

  "A mapper that marks the nested base" should "write and read the nested implementation" in {
    val inner = marking(classOf[first.Box.Inner], classOf[InnerMixin])
    val json = inner.writeValueAsString(first.Box.Shared("x"): first.Box.Inner)
    json shouldEqual taggedShared
    inner.readValue(json, classOf[first.Box.Inner]) shouldEqual first.Box.Shared("x")
  }

  "A mapper that marks the whole hierarchy" should "not read a name that means nothing at its own root" in {
    // for this mapper the root of `Box.Inner` is `Top`, where `Shared` names the top-level
    // implementation - which is not an `Inner`, so there is nothing to read. The entry the mapper
    // above left in the shared cache must not be taken for an answer to this question.
    val top = marking(classOf[first.Top], classOf[TopMixin])
    an[Exception] should be thrownBy top.readValue(taggedShared, classOf[first.Box.Inner])
  }

  "A mapper that marks the nested base" should "read its own output after another mapper resolved the name" in {
    // the same pair the other way round, on the copy no earlier test has touched
    val top = marking(classOf[second.Top], classOf[TopMixin])
    an[Exception] should be thrownBy top.readValue(taggedShared, classOf[second.Box.Inner])

    val inner = marking(classOf[second.Box.Inner], classOf[InnerMixin])
    inner.readValue(taggedShared, classOf[second.Box.Inner]) shouldEqual second.Box.Shared("x")
  }
}
