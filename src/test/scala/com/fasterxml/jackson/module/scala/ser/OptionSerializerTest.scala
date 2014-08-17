package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.databind.node.JsonNodeType
import com.fasterxml.jackson.module.jsonSchema.JsonSchema
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.annotation._
import scala.annotation.meta.{field, getter}
import com.fasterxml.jackson.module.jsonSchema.factories.SchemaFactoryWrapper
import com.fasterxml.jackson.module.scala.experimental.{ScalaObjectMapper, RequiredPropertiesSchemaModule}
import com.fasterxml.jackson.databind.{ObjectMapper, JsonNode}
import scala.collection.JavaConverters._

object OptionSerializerTest
{
  class NonEmptyOptions {

    //@JsonProperty
    @(JsonInclude)(JsonInclude.Include.NON_EMPTY)
    val none = None

    //@JsonProperty
    @(JsonInclude @getter)(JsonInclude.Include.NON_EMPTY)
    val some = Some(1)

  }

  case class OptionSchema(stringValue: Option[String])

  case class MixedOptionSchema(@JsonProperty(required=true) nonOptionValue: String, stringValue: Option[String])
  case class WrapperOfOptionOfJsonNode(jsonNode: Option[JsonNode])

  @JsonSubTypes(Array(new JsonSubTypes.Type(classOf[Impl])))
  trait Base

  @JsonTypeName("impl")
  case class Impl() extends Base

  class BaseHolder(
    private var _base: Option[Base]
  ) {
    @(JsonTypeInfo @field)(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property="$type")
    def base = _base
    def base_=(base:Option[Base]) { _base = base }
  }

}

@RunWith(classOf[JUnitRunner])
class OptionSerializerTest extends SerializerTest {
  import OptionSerializerTest._

  lazy val module = DefaultScalaModule

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
    val nonNullMapper = mapper
    nonNullMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
    nonNullMapper.writeValueAsString(new NonNullOption()) should be ("{}")
  }

  it should "generate correct schema for options" in {
    val schema = mapper.generateJsonSchema(classOf[OptionSchema])
    val schemaNode = schema.getSchemaNode

    val typeNode = schemaNode.path("type")
    typeNode should not be 'missingNode
    typeNode should have (
      'nodeType (JsonNodeType.STRING),
      'textValue ("object")
    )

    val stringValueTypeNode = schemaNode.path("properties").path("stringValue").path("type")
    stringValueTypeNode should not be 'missingNode
    stringValueTypeNode should have (
      'nodeType (JsonNodeType.STRING),
      'textValue ("string")
    )
  }

  it should "generate correct schema for options using the new jsonSchema jackson module" in {
    val visitor = new SchemaFactoryWrapper()
    mapper.acceptJsonFormatVisitor(mapper.constructType(classOf[OptionSchema]), visitor)

    val schema = visitor.finalSchema
    schema should be an 'objectSchema
    val props = schema.asObjectSchema().getProperties.asScala
    props should contain key "stringValue"
    val stringValue = props("stringValue")
    stringValue should be a 'stringSchema
  }

  it should "mark as required the non-Option fields" in {
    val visitor = new SchemaFactoryWrapper()
    mapper.acceptJsonFormatVisitor(mapper.constructType(classOf[MixedOptionSchema]), visitor)

    val schema = visitor.finalSchema()
    schema should be an 'objectSchema
    val props = schema.asObjectSchema().getProperties.asScala
    props should contain key "stringValue"
    val stringValue = props("stringValue")
    stringValue should be a 'stringSchema
    stringValue.getRequired shouldBe null

    props should contain key "nonOptionValue"
    val nonOptionValue: JsonSchema = props("nonOptionValue")
    nonOptionValue should be a 'stringSchema
    nonOptionValue.getRequired shouldBe true
  }

  it should "support reversing the default for required properties in schema" in {
    case class DefaultOptionSchema(nonOptionValue: String, stringValue: Option[String])

    val m = mapper
    m.registerModule(new RequiredPropertiesSchemaModule{})

    val visitor = new SchemaFactoryWrapper()
    m.acceptJsonFormatVisitor(mapper.constructType(classOf[DefaultOptionSchema]), visitor)

    val schema = visitor.finalSchema()
    schema should be an 'objectSchema
    val props = schema.asObjectSchema().getProperties.asScala
    props should contain key "stringValue"
    val stringValue = props("stringValue")
    stringValue should be a 'stringSchema
    stringValue.getRequired shouldBe null

    props should contain key "nonOptionValue"
    val nonOptionValue: JsonSchema = props("nonOptionValue")
    nonOptionValue should be a 'stringSchema
    nonOptionValue.getRequired shouldBe true
  }

  it should "serialize contained JsonNode correctly" in {
    val json: String = """{"prop":"value"}"""
    val tree: JsonNode = mapper.readTree(json)
    val wrapperOfOptionOfJsonNode = WrapperOfOptionOfJsonNode(Some(tree))

    val actualJson: String = mapper.writeValueAsString(wrapperOfOptionOfJsonNode)

    actualJson shouldBe """{"jsonNode":{"prop":"value"}}"""
  }

  it should "propagate type information" in {
    val json: String = """{"base":{"$type":"impl"}}"""
    mapper.writeValueAsString(new BaseHolder(Some(Impl()))) shouldBe json
  }

  it should "support default typing" in {
    case class User(name: String, email:Option[String] = None)
    val mapper = new ObjectMapper with ScalaObjectMapper
    mapper.registerModule(DefaultScalaModule)
    mapper.enableDefaultTyping()
    mapper.writeValueAsString(User("John Smith", Some("john.smith@unit.uk"))) shouldBe """{"name":"John Smith","email":"john.smith@unit.uk"}"""
  }

}

class NonNullOption {
  @JsonProperty var foo: Option[String] = None
}

