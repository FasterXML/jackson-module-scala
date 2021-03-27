package com.fasterxml.jackson.module.scala.ser

import java.util
import com.fasterxml.jackson.annotation._
import com.fasterxml.jackson.databind.jsontype.DefaultBaseTypeLimitingValidator
import com.fasterxml.jackson.databind.node.JsonNodeType
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.jsonSchema.JsonSchema
import com.fasterxml.jackson.module.jsonSchema.factories.SchemaFactoryWrapper
import com.fasterxml.jackson.module.scala.{DefaultScalaModule, JacksonModule, RequiredPropertiesSchemaModule}

import scala.annotation.meta.{field, getter}
import scala.collection.JavaConverters._

object OptionSerializerTest {
  class NonEmptyOptions {
    //@JsonProperty
    @(JsonInclude)(JsonInclude.Include.NON_EMPTY)
    val none: None.type = None

    //@JsonProperty
    @(JsonInclude @getter)(JsonInclude.Include.NON_EMPTY)
    val some = Some(1)
  }

  case class OptionGeneric[T](data: Option[T])

  case class OptionSchema(stringValue: Option[String])

  case class MixedOptionSchema(@JsonProperty(required = true) nonOptionValue: String, stringValue: Option[String])
  case class WrapperOfOptionOfJsonNode(jsonNode: Option[JsonNode])

  @JsonSubTypes(Array(new JsonSubTypes.Type(classOf[Impl])))
  trait Base

  @JsonTypeName("impl")
  case class Impl() extends Base

  class BaseHolder(private var _base: Option[Base]) {
    @(JsonTypeInfo@field)(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "$type")
    def base: Option[Base] = _base
    def base_=(base: Option[Base]): Unit = {
      _base = base
    }
  }

  // For issue #240, for some reason it works fine when these classes are part
  // of the object.
  trait M1
  case class F1(label: String) extends M1
  case class C1(m: Option[M1])
}

// For issue #240, these need to be outside of the object to reproduce
// the problem.
trait M2
case class F2(label: String) extends M2
case class C2(m: Option[M2])

class OptionSerializerTest extends SerializerTest {
  import OptionSerializerTest._

  lazy val module: JacksonModule = DefaultScalaModule

  "An ObjectMapper with OptionSerializer" should "serialize an Option[Int]" in {
    val noneOption: Option[Int] = None
    serialize(Option(1)) should be ("1")
    serialize(Some(1)) should be ("1")
    serialize(noneOption) should be ("null")
  }

  it should "serialize an Option[String]" in {
    val noneOption: Option[String] = None
    serialize(Option("foo")) should be ("\"foo\"")
    serialize(Some("foo")) should be ("\"foo\"")
    serialize(noneOption) should be ("null")
  }

  it should "serialize an Option[java.lang.Integer]" in {
    val noneOption: Option[java.lang.Integer] = None
    val someInt: Option[java.lang.Integer] = Some(1)
    serialize(someInt) should be ("1")
    serialize(noneOption) should be ("null")
  }

  it should "serialize concrete type when using Option[Trait] (in object)" in {
    // Additional test case for #240, for some reason this test case works fine.
    // However, if the classes are moved outside of the object then it starts
    // breaking.
    serialize(C1(Some(F1("foo")))) should be ("""{"m":{"label":"foo"}}""")
  }

  it should "serialize concrete type when using Option[Trait]" in {
    // See https://github.com/FasterXML/jackson-module-scala/issues/240 for more information.
    // This test case reproduces the problem in the ticket.
    serialize(C2(Some(F2("foo")))) should be ("""{"m":{"label":"foo"}}""")
  }

  it should "serialize an Option[java.lang.Integer] when accessed on a class" in {
    case class Review(score: java.lang.Integer)
    val r1: Review = null
    val r2: Review = Review(1)
    def score1 = Option(r1) map { _.score }
    def score2 = Option(r2) map { _.score }
    serialize(score1) should be ("null")
    serialize(score2) should be ("1")
  }

  it should "honor JsonInclude(NON_EMPTY)" in {
    serialize(new NonEmptyOptions) should be ("""{"some":1}""")
  }

  it should "honor JsonInclude.Include.NON_NULL" in {
    // See https://github.com/FasterXML/jackson-datatype-jdk8/issues/1 for more information.
    newMapper
      .setSerializationInclusion(JsonInclude.Include.NON_NULL)
      .writeValueAsString(new NonNullOption()) should be ("""{"foo":null}""")
  }

  it should "generate correct schema for options" in {
    val schema = newMapper.generateJsonSchema(classOf[OptionSchema])
    val schemaNode = schema.getSchemaNode

    val typeNode = schemaNode.path("type")
    typeNode should not be Symbol("missingNode")
    typeNode should have (
      Symbol("nodeType") (JsonNodeType.STRING),
      Symbol("textValue") ("object")
    )

    val stringValueTypeNode = schemaNode.path("properties").path("stringValue").path("type")
    stringValueTypeNode should not be Symbol("missingNode")
    stringValueTypeNode should have (
      Symbol("nodeType") (JsonNodeType.STRING),
      Symbol("textValue") ("string")
    )
  }

  it should "generate correct schema for options using the new jsonSchema jackson module" in {
    val visitor = new SchemaFactoryWrapper()
    newMapper.acceptJsonFormatVisitor(newMapper.constructType(classOf[OptionSchema]), visitor)

    val schema = visitor.finalSchema
    schema should be an Symbol("objectSchema")
    val props = schema.asObjectSchema().getProperties.asScala
    props should contain key "stringValue"
    val stringValue = props("stringValue")
    stringValue should be a Symbol("stringSchema")
  }

  it should "mark as required the non-Option fields" in {
    val visitor = new SchemaFactoryWrapper()
    newMapper.acceptJsonFormatVisitor(newMapper.constructType(classOf[MixedOptionSchema]), visitor)

    val schema = visitor.finalSchema()
    schema should be an Symbol("objectSchema")
    val props = schema.asObjectSchema().getProperties.asScala
    props should contain key "stringValue"
    val stringValue = props("stringValue")
    stringValue should be a Symbol("stringSchema")
    stringValue.getRequired shouldBe null

    props should contain key "nonOptionValue"
    val nonOptionValue: JsonSchema = props("nonOptionValue")
    nonOptionValue should be a Symbol("stringSchema")
    nonOptionValue.getRequired shouldBe true
  }

  it should "support reversing the default for required properties in schema" in {
    case class DefaultOptionSchema(nonOptionValue: String, stringValue: Option[String])

    val m = newMapper
    m.registerModule(new RequiredPropertiesSchemaModule{})

    val visitor = new SchemaFactoryWrapper()
    m.acceptJsonFormatVisitor(newMapper.constructType(classOf[DefaultOptionSchema]), visitor)

    val schema = visitor.finalSchema()
    schema should be an Symbol("objectSchema")
    val props = schema.asObjectSchema().getProperties.asScala
    props should contain key "stringValue"
    val stringValue = props("stringValue")
    stringValue should be a Symbol("stringSchema")
    stringValue.getRequired shouldBe null

    props should contain key "nonOptionValue"
    val nonOptionValue: JsonSchema = props("nonOptionValue")
    nonOptionValue should be a Symbol("stringSchema")
    nonOptionValue.getRequired shouldBe true
  }

  it should "serialize contained JsonNode correctly" in {
    val json: String = """{"prop":"value"}"""
    val tree: JsonNode = newMapper.readTree(json)
    val wrapperOfOptionOfJsonNode = WrapperOfOptionOfJsonNode(Some(tree))

    val actualJson: String = newMapper.writeValueAsString(wrapperOfOptionOfJsonNode)

    actualJson shouldBe """{"jsonNode":{"prop":"value"}}"""
  }

  it should "propagate type information" in {
    val json: String = """{"base":{"$type":"impl"}}"""
    newMapper.writeValueAsString(new BaseHolder(Some(Impl()))) shouldBe json
  }

  it should "support default typing" in {
    case class User(name: String, email: Option[String] = None)
    val mapper = newMapper
    val user = User("John Smith", Some("john.smith@unit.uk"))
    val expected = """{"name":"John Smith","email":"john.smith@unit.uk"}"""
    mapper.writeValueAsString(user) shouldEqual expected
    mapper.activateDefaultTyping(mapper.getPolymorphicTypeValidator)
    mapper.writeValueAsString(user) shouldEqual expected
    mapper.activateDefaultTyping(new DefaultBaseTypeLimitingValidator)
    mapper.writeValueAsString(user) shouldEqual expected
  }

  it should "serialize JsonTypeInfo info in Option[T]" in {
    val apple = Apple("green")

    val basket = SingleFruitBasket(fruit = Some(apple))

    serialize(basket) should be ("""{"fruit":{"type":"Apple","color":"green"}}""")
  }

  it should "serialize JsonTypeInfo info in Option[Seq[T]]" in {
    val apple = Apple("green")

    val basket = OrderedFruitBasket(fruits = Some(Seq(apple)))

    serialize(basket) should be ("""{"fruits":[{"type":"Apple","color":"green"}]}""")
  }

  it should "serialize JsonTypeInfo info in Option[Set[T]]" in {
    val apple = Apple("green")

    val basket = NonOrderedFruitBasket(fruits = Some(Set(apple)))

    serialize(basket) should be ("""{"fruits":[{"type":"Apple","color":"green"}]}""")
  }

  it should "serialize JsonTypeInfo info in Option[java.util.List[T]]" in {
    val apple = Apple("green")

    val javaFruits = new util.ArrayList[Fruit]()
    javaFruits.add(apple)
    val basket = JavaTypedFruitBasket(fruits = Some(javaFruits))

    serialize(basket) should be ("""{"fruits":[{"type":"Apple","color":"green"}]}""")
  }

  it should "serialize with content inclusion ALWAYS" in {
    val mapper = newMapper
      .setDefaultPropertyInclusion(JsonInclude.Value.construct(JsonInclude.Include.NON_ABSENT, JsonInclude.Include.ALWAYS))
    serialize(OptionGeneric(Option("green")), mapper) should be ("""{"data":"green"}""")
  }

  it should "serialize with content inclusion ALWAYS, when null" in {
    val mapper = newMapper
      .setDefaultPropertyInclusion(JsonInclude.Value.construct(JsonInclude.Include.ALWAYS, JsonInclude.Include.ALWAYS))
    serialize(OptionGeneric(null), mapper) should be ("""{"data":null}""")
  }

  it should "serialize with content inclusion ALWAYS, when None" in {
    val mapper = newMapper
      .setDefaultPropertyInclusion(JsonInclude.Value.construct(JsonInclude.Include.ALWAYS, JsonInclude.Include.ALWAYS))
    serialize(OptionGeneric(None), mapper) should be ("""{"data":null}""")
  }

  it should "serialize with content inclusion ALWAYS, when empty value" in {
    val mapper = newMapper
      .setDefaultPropertyInclusion(JsonInclude.Value.construct(JsonInclude.Include.ALWAYS, JsonInclude.Include.ALWAYS))
    serialize(OptionGeneric(Some("")), mapper) should be ("""{"data":""}""")
  }

  it should "serialize with content inclusion NON_NULL" in {
    val mapper = newMapper
      .setDefaultPropertyInclusion(JsonInclude.Value.construct(JsonInclude.Include.NON_ABSENT, JsonInclude.Include.NON_NULL))
    serialize(OptionGeneric(Option("green")), mapper) should be ("""{"data":"green"}""")
  }

  it should "serialize with content inclusion NON_NULL, excludes null" in {
    val mapper = newMapper
      .setDefaultPropertyInclusion(JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL))
    serialize(OptionGeneric(null), mapper) should be ("""{}""")
  }

  it should "serialize with content inclusion NON_NULL, includes None" in {
    val mapper = newMapper
      .setDefaultPropertyInclusion(JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL))
    serialize(OptionGeneric(None), mapper) should be ("""{"data":null}""")
  }

  it should "serialize with content inclusion NON_NULL, includes empty value" in {
    val mapper = newMapper
      .setDefaultPropertyInclusion(JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL))
    serialize(OptionGeneric(Some("")), mapper) should be ("""{"data":""}""")
  }

  it should "serialize with content inclusion NON_ABSENT" in {
    val mapper = newMapper
      .setDefaultPropertyInclusion(JsonInclude.Value.construct(JsonInclude.Include.NON_ABSENT, JsonInclude.Include.NON_ABSENT))
    serialize(OptionGeneric(Option("green")), mapper) should be ("""{"data":"green"}""")
  }

  it should "serialize with content inclusion NON_ABSENT, excludes null" in {
    val mapper = newMapper
      .setDefaultPropertyInclusion(JsonInclude.Value.construct(JsonInclude.Include.NON_ABSENT, JsonInclude.Include.NON_ABSENT))
    serialize(OptionGeneric(null), mapper) should be ("""{}""")
  }

  it should "serialize with content inclusion NON_ABSENT, excludes None" in {
    val mapper = newMapper
      .setDefaultPropertyInclusion(JsonInclude.Value.construct(JsonInclude.Include.NON_ABSENT, JsonInclude.Include.NON_ABSENT))
    serialize(OptionGeneric(None), mapper) should be ("""{}""")
  }

  it should "serialize with content inclusion NON_ABSENT, includes empty value" in {
    val mapper = newMapper
      .setDefaultPropertyInclusion(JsonInclude.Value.construct(JsonInclude.Include.NON_ABSENT, JsonInclude.Include.NON_ABSENT))
    serialize(OptionGeneric(Some("")), mapper) should be ("""{"data":""}""")
  }

  it should "serialize with content inclusion NON_EMPTY" in {
    val mapper = newMapper
      .setDefaultPropertyInclusion(JsonInclude.Value.construct(JsonInclude.Include.NON_ABSENT, JsonInclude.Include.NON_EMPTY))
    serialize(OptionGeneric(Option("green")), mapper) should be ("""{"data":"green"}""")
  }

  it should "serialize with content inclusion NON_EMPTY, excludes null" in {
    val mapper = newMapper
      .setDefaultPropertyInclusion(JsonInclude.Value.construct(JsonInclude.Include.NON_EMPTY, JsonInclude.Include.NON_EMPTY))
    serialize(OptionGeneric(null), mapper) should be ("""{}""")
  }

  it should "serialize with content inclusion NON_EMPTY, excludes None" in {
    val mapper = newMapper
      .setDefaultPropertyInclusion(JsonInclude.Value.construct(JsonInclude.Include.NON_EMPTY, JsonInclude.Include.NON_EMPTY))
    serialize(OptionGeneric(None), mapper) should be ("""{}""")
  }

  it should "serialize with content inclusion NON_EMPTY, excludes empty value" in {
    val mapper = newMapper
      .setDefaultPropertyInclusion(JsonInclude.Value.construct(JsonInclude.Include.NON_EMPTY, JsonInclude.Include.NON_EMPTY))
    serialize(OptionGeneric(Some("")), mapper) should be ("""{}""")
  }

  it should "emit [] for empty list with content inclusion NON_EMPTY" in {
    val mapper = newMapper
      .setDefaultPropertyInclusion(JsonInclude.Value.construct(JsonInclude.Include.NON_ABSENT, JsonInclude.Include.ALWAYS))
    serialize(OptionGeneric(Option(List.empty)), mapper) should be ("""{"data":[]}""")
  }

  it should "serialize Some(null) to null" in {
    serialize(Some(null), newMapper) should be("null")
  }
}

class NonNullOption {
  @JsonProperty var foo: Option[String] = None
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
trait Fruit {
  def color: String
}

case class Apple(color: String) extends Fruit
case class SingleFruitBasket(fruit: Option[Fruit])
case class OrderedFruitBasket(fruits: Option[Seq[Fruit]])
case class NonOrderedFruitBasket(fruits: Option[Set[Fruit]])
case class JavaTypedFruitBasket(fruits: Option[java.util.List[Fruit]])
