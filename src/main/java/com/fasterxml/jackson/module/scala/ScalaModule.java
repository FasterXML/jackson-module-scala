package com.fasterxml.jackson.module.scala;

import com.fasterxml.jackson.module.scala.deser.ScalaDeserializers;
import com.fasterxml.jackson.module.scala.deser.SeqTypeModifier;
import com.fasterxml.jackson.module.scala.ser.ScalaSerializers;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.*;

public class ScalaModule extends Module
{
    private final String NAME = "ScalaModule";
    
    // Should externalize this somehow
    private final static Version VERSION = new Version(0, 1, 0, null); // 0.1.0
    
    /**
     * Enumeration that defines all togglable features this module -- none yet
     */
    public enum Feature {
        BOGUS(false) // placeholder
        ;

        final boolean _defaultState;
        final int _mask;
        
        // Method that calculates bit set (flags) of all features enabled by default
        public static int collectDefaults()
        {
            int flags = 0;
            for (Feature f : values()) {
                if (f.enabledByDefault()) {
                    flags |= f.getMask();
                }
            }
            return flags;
        }
        
        private Feature(boolean defaultState) {
            _defaultState = defaultState;
            _mask = (1 << ordinal());
        }
        
        public boolean enabledByDefault() { return _defaultState; }
        public int getMask() { return _mask; }
    }

    protected final static int DEFAULT_FEATURES = Feature.collectDefaults();
    
    /**
     * Bit flag composed of bits that indicate which {@link Feature}s are enabled.
     */
    protected int _moduleFeatures = DEFAULT_FEATURES;
    
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */
    
    public ScalaModule() { }

    @Override public String getModuleName() { return NAME; }
    @Override public Version version() { return VERSION; }

    @Override
    public void setupModule(Module.SetupContext context)
    {
        context.addDeserializers(new ScalaDeserializers());
        context.addSerializers(new ScalaSerializers());
        context.addTypeModifier(new SeqTypeModifier());
    }

    /*
    /**********************************************************
    /* Extended API, configuration
    /**********************************************************
     */

    public ScalaModule enable(Feature f) {
        _moduleFeatures |= f.getMask();
        return this;
    }

    public ScalaModule disable(Feature f) {
        _moduleFeatures &= ~f.getMask();
        return this;
    }

    public final boolean isEnabled(Feature f) {
        return (_moduleFeatures & f.getMask()) != 0;
    }

    public ScalaModule configure(Feature f, boolean state) {
        if (state) {
            enable(f);
        } else {
            disable(f);
        }
        return this;
    }
}
