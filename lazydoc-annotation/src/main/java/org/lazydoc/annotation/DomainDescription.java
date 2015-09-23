package org.lazydoc.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This annotation defines the domain of the operations defined in the controller.
 * The domain can also contain subdomains when there is the need to have more detailed descriptions.
 *
 *
 * @author Marc Eckart
 *
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface DomainDescription {

	/**
	 * The name of the domain
	 *
	 * @return The name of the domain.
	 */
	String name();

	/**
	 * The short description can be used in printers to describe in a short
	 * what the domain is about
	 *
	 * @return A short description of the domain.
	 */
	String shortDescription() default "";

	/**
	 * The description of the domain provides a more detailed content what the domain is about
	 *
	 * @return The description of the domain.
	 */
	String description() default "";

	/**
	 * The order of the domain can be used to define the order of the different domains.
	 *
	 *
	 * @return The order of the domain.
	 */
	int order() default 0;

	SubDomainDescription subDomain() default @SubDomainDescription(name = "");

	ExternalDocumentation[] externalDocumentation() default {};

	ErrorDescription[] errors() default {};

}
