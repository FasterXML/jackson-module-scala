package tools.jackson.module.scala;

import com.fasterxml.jackson.annotation.JacksonAnnotation;
import tools.jackson.core.type.TypeReference;
import scala.Enumeration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotation
public @interface JsonScalaEnumeration {
    Class<? extends TypeReference<? extends Enumeration>> value();
}
