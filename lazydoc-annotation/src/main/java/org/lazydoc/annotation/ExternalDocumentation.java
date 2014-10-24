package org.lazydoc.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ExternalDocumentation {

	String location() default "";

	InsertPosition postion() default InsertPosition.TOP;
}
