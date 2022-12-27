package com.fasterxml.jackson.databind.cfg;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

public class Tester {
    public static void main(String[] args) {
        JsonTypeInfoValue infoValue = new JsonTypeInfoValue(JsonTypeInfo.Id.NAME,
                JsonTypeInfo.As.PROPERTY, "xyz", null, false);
       infoValue.asAnnotation();
    }
}
