package org.lazydoc.annotation;

/**
 * Created by m.eckart on 15.09.2015.
 */
public @interface PropertyMapDescription {

    String keyDescription() default "";
    
    String valueDescription() default "";
}
