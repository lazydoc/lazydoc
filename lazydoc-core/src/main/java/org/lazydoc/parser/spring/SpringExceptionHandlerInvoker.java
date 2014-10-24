package org.lazydoc.parser.spring;

import java.lang.reflect.Method;

import org.springframework.web.bind.annotation.ExceptionHandler;

public interface SpringExceptionHandlerInvoker {

	public abstract String getErrorMessageFromExceptionHandler(Method method,
			Object controllerInstance, ExceptionHandler exceptionHandler);

}