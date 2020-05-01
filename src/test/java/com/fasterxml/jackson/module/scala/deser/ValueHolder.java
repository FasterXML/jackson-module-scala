package __foursquare_shaded__.com.fasterxml.jackson.module.scala.deser;

import __foursquare_shaded__.com.fasterxml.jackson.annotation.JsonCreator;

public class ValueHolder {
    public final long internalValue;

    private ValueHolder(long internalValue) {
        this.internalValue = internalValue;
    }

    @JsonCreator
    public static ValueHolder parse(String value) {
        return new ValueHolder(Long.parseLong(value));
    }
}
