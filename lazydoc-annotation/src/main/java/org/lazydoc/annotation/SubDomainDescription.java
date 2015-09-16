package org.lazydoc.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface SubDomainDescription {

	int order() default 0;

	String name();

	String shortDescription() default "";

	String description() default "";

	ExternalDocumentation[] externalDocumentation() default {};
}
