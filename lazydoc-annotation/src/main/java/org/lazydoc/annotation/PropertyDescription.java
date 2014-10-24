package org.lazydoc.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface PropertyDescription {

	String description() default "";

	boolean addPossibleEnumValues() default true;

	Class<?> type() default void.class;

	boolean required() default false;

	boolean onlyRequest() default false;

	boolean onlyResponse() default false;

}
