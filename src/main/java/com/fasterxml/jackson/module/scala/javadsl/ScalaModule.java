package com.fasterxml.jackson.module.scala.javadsl;

import com.fasterxml.jackson.module.scala.*;
import com.fasterxml.jackson.module.scala.deser.*;
import com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule;
import com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule$;

public final class ScalaModule {
    public static com.fasterxml.jackson.module.scala.ScalaModule.Builder builder() {
        return ScalaModule$.MODULE$.builder();
    }

    public static EitherModule eitherModule() {
        return EitherModule$.MODULE$;
    }

    /**
     * EnumModule is not available in Scala 2.x - as it relates to Scala 3 <code>enum</code>s.
     */
    public static JacksonModule enumModule() {
        try {
            Class<?> objectClass = Class.forName("tools.jackson.module.scala.EnumModule$");
            return (JacksonModule) objectClass.getDeclaredField("MODULE$").get(null);
        } catch (Throwable t) {
            return null;
        }
    }

    public static EnumerationModule enumerationModule() {
        return EnumerationModule$.MODULE$;
    }

    public static IteratorModule iteratorModule() {
        return IteratorModule$.MODULE$;
    }

    public static IterableModule iterableModule() {
        return IterableModule$.MODULE$;
    }

    public static OptionModule optionModule() {
        return OptionModule$.MODULE$;
    }

    public static TupleModule tupleModule() {
        return TupleModule$.MODULE$;
    }

    public static MapModule mapModule() {
        return MapModule$.MODULE$;
    }

    public static SetModule setModule() {
        return SetModule$.MODULE$;
    }

    public static SymbolModule symbolModule() {
        return SymbolModule$.MODULE$;
    }

    public static ScalaAnnotationIntrospectorModule scalaAnnotationIntrospectorModule() {
        return ScalaAnnotationIntrospectorModule$.MODULE$;
    }

    public static ScalaNumberDeserializersModule scalaNumberDeserializersModule() {
        return ScalaNumberDeserializersModule$.MODULE$;
    }

    public static ScalaObjectDeserializerModule scalaObjectDeserializerModule() {
        return ScalaObjectDeserializerModule$.MODULE$;
    }

    public static UntypedObjectDeserializerModule untypedObjectDeserializerModule() {
        return UntypedObjectDeserializerModule$.MODULE$;
    }

    private ScalaModule() { }
}
