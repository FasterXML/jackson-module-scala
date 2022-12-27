package com.fasterxml.jackson.databind.cfg;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.lang.annotation.Annotation;

public class JsonTypeInfoValue {

    private final JsonTypeInfo.Id _use;
    private final JsonTypeInfo.As _include;
    private final String _property;
    private final Class<?> _defaultImpl;
    private final boolean _visible;

    public JsonTypeInfoValue(JsonTypeInfo.Id use, JsonTypeInfo.As include, String property,
                             Class<?> defaultImpl, boolean visible) {
        _use = use;
        _include = include;
        _property = property;
        _defaultImpl = defaultImpl;
        _visible = visible;
    }

    public JsonTypeInfo asAnnotation() {
        return new JsonTypeInfo() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return JsonTypeInfo.class;
            }

            @Override
            public Id use() {
                return _use;
            }

            @Override
            public As include() {
                return _include;
            }

            @Override
            public String property() {
                return _property;
            }

            @Override
            public Class<?> defaultImpl() {
                return _defaultImpl;
            }

            @Override
            public boolean visible() {
                return _visible;
            }
        };
    }
}
