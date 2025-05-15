package tools.jackson.module.scala;

import tools.jackson.core.FormatFeature;

/**
 * Enumeration that defines all togglable features for jackson-module-scala.
 */
public enum ScalaFeature implements FormatFeature
{
    /**
     * Feature that enables support for default values specified in constructors.
     * When enabled, if a constructor has default values for some of its parameters,
     * and those parameters are not present in the JSON input, the default values
     * will be used to fill in the missing values.
     * <p>
     * Default setting is {@code true}.
     */
    APPLY_DEFAULT_VALUES_WHEN_DESERIALIZING(true),

    /**
     * Feature that enables jackson-module-scala Scala 2 users to use classes
     * compiled with Scala 3. The Scala 2 runtime jars that are used will still
     * need to be able to load Scala 3 classes, but this feature will allow
     * Jackson to recognize them. There is a small overhead for this.
     * <p>
     * Default setting is {@code true}.
     */
    SUPPORT_SCALA3_CLASSES(true),

    ;

    /**
     * Method that calculates bit set (flags) of all features that
     * are enabled by default.
     */
    public static int collectDefaults()
    {
        int flags = 0;
        for (ScalaFeature f : values()) {
            if (f.enabledByDefault()) {
                flags |= f.getMask();
            }
        }
        return flags;
    }

    private final boolean _defaultState;
    private final int _mask;

    private ScalaFeature(final boolean defaultState) {
        _defaultState = defaultState;
        _mask = (1 << ordinal());
    }

    @Override public boolean enabledByDefault() { return _defaultState; }
    @Override public int getMask() { return _mask; }
    @Override public boolean enabledIn(int flags) { return (flags & getMask()) != 0; }
}
