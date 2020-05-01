package __foursquare_shaded__.com.fasterxml.jackson.module.scala;

import __foursquare_shaded__.com.fasterxml.jackson.annotation.JacksonAnnotation;
import __foursquare_shaded__.com.fasterxml.jackson.core.type.TypeReference;
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
