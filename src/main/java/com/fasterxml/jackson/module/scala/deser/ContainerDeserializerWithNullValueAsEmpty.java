package com.fasterxml.jackson.module.scala.deser;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.std.ContainerDeserializerBase;

/**
 * Internal Usage only
 */
abstract class ContainerDeserializerWithNullValueAsEmpty<T> extends ContainerDeserializerBase<T> {

    protected ContainerDeserializerWithNullValueAsEmpty(JavaType selfType) {
        super(selfType);
    }

    @Override
    public T getNullValue(DeserializationContext ctxt) throws JsonMappingException {
        return (T) getEmptyValue(ctxt);
    }
}
