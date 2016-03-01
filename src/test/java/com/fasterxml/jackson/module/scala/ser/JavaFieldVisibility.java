package com.fasterxml.jackson.module.scala.ser;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JavaFieldVisibility extends JavaAFieldVisibility {
    public String baz = "not-visible";
    @JsonProperty
    public String zip = "visible";
}
