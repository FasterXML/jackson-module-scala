package com.fasterxml.jackson.module.scala;

/**
 * @deprecated Use {@link DefaultScalaModule}
 */
@Deprecated
public class ScalaModule extends DefaultScalaModule
{
    private static final String NAME = "ScalaModule";
    
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */
    
    public ScalaModule() { }

    @Override public String getModuleName() { return NAME; }
}
