package org.lazydoc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationDescription {

	int order() default 0;

	String description();

	String shortDescription() default "";

	String nickname() default "";

	String notes() default "";

	String staticRequestSample() default  "";

	ExternalDocumentation[] externalDocumentation() default {};

}
