package org.lazydoc.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface EnumDescription {

    int order() default Integer.MAX_VALUE;

    String value() default "";

}
