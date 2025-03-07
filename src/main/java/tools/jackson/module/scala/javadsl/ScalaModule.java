package tools.jackson.module.scala.javadsl;

import tools.jackson.module.scala.*;
import tools.jackson.module.scala.deser.*;
import tools.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public final class ScalaModule {
    public static tools.jackson.module.scala.ScalaModule.Builder builder() {
        return ScalaModule$.MODULE$.builder();
    }

    public static EitherModule eitherModule() {
        return EitherModule$.MODULE$;
    }

    public static JacksonModule enumModule() {
        try {
            Class<?> objectClass = Class.forName("tools.jackson.module.scala.EnumModule$");
            VarHandle varHandle = MethodHandles.publicLookup().findStaticVarHandle(
                    objectClass, "MODULE$", objectClass);
            return (JacksonModule) varHandle.get();
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
        return ScalaAnnotationIntrospectorModule.newStandaloneInstance();
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
}
