package org.lazydoc.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ResponseDescription {

	String description() default "";

	String staticSample() default "";

	String simpleTypeDescription() default "";

	Class<?> type() default void.class;


}
