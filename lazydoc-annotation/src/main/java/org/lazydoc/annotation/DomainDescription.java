package org.lazydoc.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface DomainDescription {

	String name();

	String shortDescription() default "";

	String description() default "";

	int order() default 0;

	SubDomainDescription subDomain() default @SubDomainDescription(name = "");

	ExternalDocumentation externalDocumentation() default @ExternalDocumentation(location = "");
}
