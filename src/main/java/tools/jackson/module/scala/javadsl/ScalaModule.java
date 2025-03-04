package tools.jackson.module.scala.javadsl;

import tools.jackson.module.scala.*;

public final class ScalaModule {
    public static tools.jackson.module.scala.ScalaModule.Builder builder() {
        return tools.jackson.module.scala.ScalaModule$.MODULE$.builder();
    }

    public static EitherModule eitherModule() {
        return EitherModule$.MODULE$;
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
}
