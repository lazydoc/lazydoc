package org.lazydoc.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Parameter {
	String name();

	Class<?> type() default void.class;

	String description() default "";

	boolean ignoreForDocumentation() default false;
}
