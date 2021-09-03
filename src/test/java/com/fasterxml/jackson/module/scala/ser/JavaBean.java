package com.fasterxml.jackson.module.scala.ser;

public class JavaBean {
    private boolean isBooleanProperty = true;
    private boolean nameDoesNotMatter = false;
    private String isString = "value";

    public boolean isBooleanProperty() {
        return isBooleanProperty;
    }

    public void setBooleanProperty(boolean booleanProperty) {
        this.isBooleanProperty = booleanProperty;
    }

    public boolean getAnotherBooleanProperty() {
        return nameDoesNotMatter;
    }

    public void setAnotherBooleanProperty(boolean anotherBooleanProperty) {
        this.nameDoesNotMatter = anotherBooleanProperty;
    }

    public String getString() {
        return isString;
    }

    public void setString(String isString) {
        this.isString = isString;
    }
}
