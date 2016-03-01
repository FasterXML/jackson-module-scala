package com.fasterxml.jackson.module.scala.ser;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JavaAFieldVisibility implements JavaNotVisible {
    public String foo = "not visible";
    @JsonProperty
    public String bar = "visible";
}
