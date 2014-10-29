package org.lazydoc.parser.exception;

/**
 * Created by m.eckart on 29.10.2014.
 */
public class UndocumentedMethodException extends RuntimeException{

    public UndocumentedMethodException() {
    }

    public UndocumentedMethodException(String message) {
        super(message);
    }
}
