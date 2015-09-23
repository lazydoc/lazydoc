package org.lazydoc.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ErrorDescription {

	int statusCode() default 0;

	String detail() default "";

	String description() default "";

	boolean ignore() default false;
}
