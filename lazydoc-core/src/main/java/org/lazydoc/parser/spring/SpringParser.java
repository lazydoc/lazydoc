package org.lazydoc.parser.spring;

import org.apache.commons.lang3.StringUtils;
import org.lazydoc.annotation.*;
import org.lazydoc.config.Config;
import org.lazydoc.model.*;
import org.lazydoc.parser.DataTypeParser;
import org.lazydoc.parser.exception.UndocumentedMethodException;
import org.lazydoc.reporter.DocumentationReporter;
import org.lazydoc.util.Inspector;
import org.lazydoc.util.InstanceCreator;
import org.reflections.Reflections;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.removeEnd;

public class SpringParser {

    private DocumentationReporter reporter;
    private DataTypeParser dataTypeParser;

    private Map<Integer, DocDomain> domains = new TreeMap<>();
    private Set<DocError> listOfCommonErrors = new TreeSet<>();
    private Config config;

    public SpringParser(Config config, DocumentationReporter reporter,
                        DataTypeParser dataTypeParser) {
        super();
        this.config = config;
        this.reporter = reporter;
        this.dataTypeParser = dataTypeParser;
    }


    public void parseSpringControllers() {
        if (StringUtils.isNotBlank(config.getAbstractControllerClassForCommonExceptionHandlers())) {
            addCommonListOfPossibleErrors();
        }
        for (Class<?> subType : getAllRestfulControllerClasses()) {
            if (!Modifier.isAbstract(subType.getModifiers())) {
                extractControllerDetails(new TreeSet<String>(), subType);
            }
        }

    }

    private Set<Class<?>> getAllRestfulControllerClasses() {
        String packageToSearchForControllers = config.getPackageToSearchForControllers();
        if(StringUtils.isBlank(packageToSearchForControllers)) {
            throw new RuntimeException("Please provide package to search for controllers in configuration");
        }
        System.out.println("Looking up on package "+packageToSearchForControllers);
        Set<Class<?>> controllerSet = new Reflections(packageToSearchForControllers).getTypesAnnotatedWith(Controller.class);
        System.out.println("Found Controllers: "+StringUtils.join(controllerSet, ", "));
        return controllerSet;
    }

    private void extractControllerDetails(SortedSet<String> requestMappings, Class<?> controller) {
        System.out.println("Inspecting controller " + controller.getSimpleName());
        Class<?> documentation = getDocumentation(controller);
        if (excludeFromDocumentation(controller) || hasNoDocumentation(documentation, controller)) {
            return;
        }
        if (controllerIsNotAbstract(controller) && isControllerAnnotationPresent(controller)) {
            Set<DocError> errorList = getListOfPossibleErrors(controller);
            String generalRequestMapping = getGeneralRequestMapping(controller);
            for (Method method : controller.getDeclaredMethods()) {
                try {
                    if (ignoreMethodForDocumentation(generalRequestMapping, method)) {
                        reporter.addIgnoredMethod(controller, method.toString());
                        continue;
                    }
                    if (methodHasRequestMapping(method)) {
                        String path = getRequestMapping(generalRequestMapping, method);
                        requestMappings.add(path);
                        DocDomain domain = getDomain(method);
                        domain.setErrorList(errorList);
                        addOperation(method, path, domain);
                        reporter.addDocumentedMethod(controller, method.toString());
                    }
                } catch (UndocumentedMethodException ex) {
                    System.out.println("UNDOCUMENTED METHOD: "+ex.getMessage());
                    reporter.addUndocumentedMethod(controller, method.toString());
                }

            }
        }
    }

    private Set<DocError> getListOfPossibleErrors(Class<?> controller) {
        Class<?> stopInspectionAtControllerClass = null;
        if(StringUtils.isNotBlank(config.getControllerClassToStopErrorInspection())) {
            stopInspectionAtControllerClass = getClassByName(config.getControllerClassToStopErrorInspection());
        }
        return getListOfPossibleErrors(controller, stopInspectionAtControllerClass, InstanceCreator.createInstanceOf(controller), new TreeSet<DocError>());
    }

    private void addCommonListOfPossibleErrors() {
        if(StringUtils.isNotBlank(config.getAbstractControllerClassForCommonExceptionHandlers()) && StringUtils.isNotBlank(config.getInstanceControllerClassForCommonExceptionHandlers())) {
            Class<?> abstractControllerClass = getClassByName(config.getAbstractControllerClassForCommonExceptionHandlers());
            Class<?> instanceControllerClass = getClassByName(config.getInstanceControllerClassForCommonExceptionHandlers());
            listOfCommonErrors.addAll(getListOfPossibleErrors(abstractControllerClass, null, InstanceCreator.createInstanceOf(instanceControllerClass), new TreeSet<DocError>()));
        }
    }

    private Set<DocError> getListOfPossibleErrors(Class<?> controller, Class<?> stopAtAbstractController, Object controllerInstance,
                                                  Set<DocError> result) {
        if (controller.equals(Object.class) || (stopAtAbstractController != null && controller.equals(stopAtAbstractController))) {
            return result;
        } else {
            System.out.println("Inspecting error handler in controller class " + controller.getSimpleName());
            getListOfPossibleErrors(controller.getSuperclass(), stopAtAbstractController, controllerInstance, result);
        }
        for (Method method : controller.getDeclaredMethods()) {
            if (method.isAnnotationPresent(ExceptionHandler.class)) {
                System.out.println("Inspecting exception handler " + method.getName());
                HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
                ExceptionHandler exceptionHandler = method.getAnnotation(ExceptionHandler.class);
                if (method.isAnnotationPresent(ResponseStatus.class)) {
                    httpStatus = method.getAnnotation(ResponseStatus.class).value();
                }
                String errorMessage = "";
                String description = httpStatus.getReasonPhrase();
                if (method.isAnnotationPresent(ErrorDescription.class)) {
                    ErrorDescription errorDescription = method.getAnnotation(ErrorDescription.class);
                    if (errorDescription.ignore()) {
                        reporter.addIgnoredErrorHandler(controller, method.toString());
                        continue;
                    }
                    reporter.addDocumentedErrorHandler(controller, method.toString());
                    errorMessage = errorDescription.errorMessage();
                    description = errorDescription.description();

                } else {
                    reporter.addUndocumentedErrorHandler(controller, method.toString());
                }
                if (StringUtils.isEmpty(errorMessage)) {
                    errorMessage = getErrorMessageFromExceptionHandler(method, controllerInstance, exceptionHandler);
                }
                result.add(createDocError(httpStatus.value(), errorMessage, description));
            }
        }

        return result;
    }

    private DocError createDocError(int status, String errorCode, String description) {
        DocError error = new DocError();
        error.setHttpStatus(status);
        error.setDescription(description);
        error.setErrorCode(errorCode);
        return error;
    }

    private String getErrorMessageFromExceptionHandler(Method method, Object controllerInstance, ExceptionHandler exceptionHandler) {
        if (StringUtils.isNotBlank(config.getExceptionHandlerInvoker())) {
            try {
                Class<?> invokerClass = getClassByName(config.getExceptionHandlerInvoker());
                SpringExceptionHandlerInvoker invoker = (SpringExceptionHandlerInvoker) invokerClass.newInstance();
                return invoker.getErrorMessageFromExceptionHandler(method, controllerInstance, exceptionHandler);
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException("Error invoking ExceptionHandlerInvoker " + config.getExceptionHandlerInvoker(), e);
            }
        }
        return "";
    }

    private Class<?> getClassByName(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Error getting class for name " + className, e);
        }
    }


    private Class<?> getDocumentation(Class<?> controller) {
        if (StringUtils.isNotBlank(config.getDocumentationSuffix())) {
            try {
                return getClassByName(controller.getName() + config.getDocumentationSuffix());
            } catch (RuntimeException ex) {
                return null;
            }
        } else {
            return controller;
        }
    }

    private boolean hasNoDocumentation(Class<?> documentation, Class<?> controller) {
        if (documentation == null || !documentation.isAnnotationPresent(DomainDescription.class)) {
            reporter.addUndocumentedController(controller);
            for (Method method : controller.getDeclaredMethods()) {
                if (method.isAnnotationPresent(RequestMapping.class)) {
                    reporter.addUndocumentedMethod(controller, method.toString());
                }
                if (method.isAnnotationPresent(ExceptionHandler.class)) {
                    reporter.addUndocumentedErrorHandler(controller, method.toString());
                }
            }
            return true;
        }
        return false;
    }

    private boolean excludeFromDocumentation(Class<?> controller) {
        if (controller.isAnnotationPresent(IgnoreForDocumentation.class)) {
            IgnoreForDocumentation ignoreForDocumentation = controller.getAnnotation(IgnoreForDocumentation.class);
            reporter.addIgnoredController(controller, ignoreForDocumentation.reason());
            for (Method method : controller.getDeclaredMethods()) {
                if (method.isAnnotationPresent(RequestMapping.class)) {
                    reporter.addIgnoredMethod(controller, method.toString());
                }
                if (method.isAnnotationPresent(ExceptionHandler.class)) {
                    reporter.addIgnoredErrorHandler(controller, method.toString());
                }
            }
            return true;
        }
        return false;
    }

    private boolean ignoreMethodForDocumentation(String generalRequestMapping, Method method) {
        if (ignoreMethodForDocumentationWithCustomAnnotation(method, config.getCustomAnnotationToBeIgnored())) {
            System.out.println("Ignoring method " + method.getName() + " (" + method.toString() + ") for documentation because of custom annotation " + config.getCustomAnnotationToBeIgnored());
            printMethodPathAndHttpMethod(generalRequestMapping, method);
            return true;
        }
        if (ignoreForDocumentation(method)) {
            if (methodHasRequestMapping(method)) {
                System.err.println("Ignoring method " + method.getName() + " (" + method.toString() + ") for documentation");
                printMethodPathAndHttpMethod(generalRequestMapping, method);
                return true;
            }
        }
        return false;
    }

    private void printMethodPathAndHttpMethod(String generalRequestMapping, Method method) {
        if (methodHasRequestMapping(method)) {
            System.out.println("Ignoring path " + getRequestMapping(generalRequestMapping, method) + " - " + getHttpMethod(method));
        }
    }

    @SuppressWarnings("unchecked")
    private boolean ignoreMethodForDocumentationWithCustomAnnotation(Method method, String customAnnotationToBeIgnoredName) {
        if (StringUtils.isNotBlank(customAnnotationToBeIgnoredName)) {
            Class<? extends Annotation> customAnnotationToBeIgnored = (Class<? extends Annotation>) getClassByName(customAnnotationToBeIgnoredName);
            return method.isAnnotationPresent(customAnnotationToBeIgnored);
        }
        return false;
    }

    private boolean ignoreForDocumentation(Method method) {
        Method methodFromDocumentation = getMethodFromDocumentation(method);
        if (methodFromDocumentation != null) {
            if (methodFromDocumentation.isAnnotationPresent(IgnoreForDocumentation.class)) {
                IgnoreForDocumentation ignore = methodFromDocumentation.getAnnotation(IgnoreForDocumentation.class);
                if (StringUtils.isNotBlank(ignore.ignoreDocumentationUntil())) {
                    try {
                        if (new Date().before(new SimpleDateFormat("dd.MM.yyyy").parse(ignore.ignoreDocumentationUntil()))) {
                            return true;
                        } else {
                            throw new RuntimeException("The ignore date is expired, please document the method " + method.toString());
                        }
                    } catch (ParseException e) {
                        throw new RuntimeException("The ignoreStartDate has the wrong format, should be 'dd.MM.yyyy'", e);
                    }
                } else {
                    return true;
                }
            }
            return false;
        } else {
            if (methodHasRequestMapping(method)) {
                throw new UndocumentedMethodException("No documented method " + method.getName() + " found. Maybe signature has changed?");
            } else {
                return true;
            }
        }
    }

    private DocDomain getDomain(Method method) {

        if (method == null) {
            throw new RuntimeException("No domain description found in controller");
        }
        DomainDescription domainDescription = null;
        if (method.isAnnotationPresent(DomainDescription.class)) {
            domainDescription = method.getAnnotation(DomainDescription.class);
        } else if (method.getDeclaringClass().isAnnotationPresent(DomainDescription.class)) {
            domainDescription = method.getDeclaringClass().getAnnotation(DomainDescription.class);
        }
        if (domainDescription != null) {
            return getDomain(domainDescription);
        }
        return getDomain(getMethodFromDocumentation(method));
    }

    private Method getMethodFromDocumentation(Method method) {
        if(StringUtils.isNotBlank(config.getDocumentationSuffix())) {
            Class<?> documentation = getDocumentation(method.getDeclaringClass());
            try {
                return documentation.getDeclaredMethod(method.getName(), method.getParameterTypes());
            } catch (NoSuchMethodException | SecurityException e) {
                return null;
            }
        } else {
            return method;
        }
    }

    private boolean controllerIsNotAbstract(Object subType) {
        return !Modifier.isAbstract(((Class<?>) subType).getModifiers());
    }

    private boolean isControllerAnnotationPresent(Class<?> controller) {
        return controller.isAnnotationPresent(Controller.class);
    }

    private boolean methodHasRequestMapping(Method method) {
        return method.isAnnotationPresent(RequestMapping.class);
    }

    private void addOperation(Method method, String path, DocDomain domain) {
        OperationDescription operationDescription = getOperationDescription(method);
        DocOperation operation = new DocOperation();
        operation.setHttpMethod(getHttpMethod(method));
        operation.setResponseClass(getResponseClass(method));
        operation.setNickname(method.getName());
        operation.setPath(path);
        operation.setParameters(getParametersOfMethod(method, path));
        operation.setRole(getRoleFromMethod(method));
        if (operationDescription != null) {
            if (StringUtils.isNotBlank(operationDescription.nickname())) {
                operation.setNickname(operationDescription.nickname());
            }
            operation.setShortDescription(operationDescription.shortDescription());
            operation.setSummary(operationDescription.description());
            operation.setNotes(operationDescription.notes());
            operation.setOrder(operationDescription.order());
            operation.setExternalDocumentation(operationDescription.externalDocumentation().location());
            operation.setExternalInsertPosition(operationDescription.externalDocumentation().postion());
        }
        domain.getOperations().add(operation);
    }

    private OperationDescription getOperationDescription(Method method) {
        Method documentedMethod = getMethodFromDocumentation(method);
        if (documentedMethod.isAnnotationPresent(OperationDescription.class)) {
            return documentedMethod.getAnnotation(OperationDescription.class);
        }
        throw new UndocumentedMethodException("No Operation description found at method "+method.toString());
    }

    private String getRoleFromMethod(Method method) {
        if (method.isAnnotationPresent(PreAuthorize.class)) {
            String role = method.getAnnotation(PreAuthorize.class).value();
            return role.replaceAll("hasAnyRole\\(", "").replaceAll("hasRole\\(", "").replaceAll("'", "").replaceAll(" + #mandant.", "")
                    .replaceAll("\\)", "");
        }
        return "";
    }

    private String getGeneralRequestMapping(Class<?> controller) {
        String generalRequestMapping = "";
        if (controller.isAnnotationPresent(RequestMapping.class)) {
            generalRequestMapping = controller.getAnnotation(RequestMapping.class).value()[0];
        }
        return generalRequestMapping;
    }

    private String getRequestMapping(String generalRequestMapping, Method method) {
        String methodRequestMapping = "";
        RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
        if (requestMapping.value().length > 0) {
            methodRequestMapping = requestMapping.value()[0];
        }
        return (addSlashIfNotThere(generalRequestMapping) + addSlashIfNotThere(methodRequestMapping)).replaceAll("//", "/");
    }

    private String addSlashIfNotThere(String requestMapping) {
        if (!requestMapping.startsWith("/")) {
            return "/" + requestMapping;
        }
        return requestMapping;
    }

    private DocDomain getDomain(DomainDescription description) {
        DocDomain domain = domains.get(description.order());
        if (domain == null) {
            domain = new DocDomain();
            domain.setDomain(description.name());
            domain.setDomainShortDescription(description.shortDescription());
            domain.setDescription(description.description());
            domain.setOrder(description.order());
            domain.setExternalDocumentation(description.externalDocumentation().location());
            domain.setExternalInsertPosition(description.externalDocumentation().postion());
            domain.setBasePath(config.getSwaggerBasePath());
            domain.getErrorList().addAll(listOfCommonErrors);
            domains.put(description.order(), domain);
            if (StringUtils.isNotBlank(description.subDomain().name())) {
                DocSubDomain subDomain = createDocSubDomain(description);
                domain.addSubDomain(subDomain);
                return subDomain;
            }
        } else {
            if (!domain.getDomain().equals(description.name())) {
                throw new RuntimeException("Domain names different for same order " + description.order() + ": Found: "
                        + domain.getDomain() + " - Given: " + description.name());
            } else {
                if (StringUtils.isNotBlank(description.subDomain().name())) {
                    DocSubDomain subDomain = domain.getSubDomains().get(description.subDomain().order());
                    if (subDomain == null) {
                        subDomain = createDocSubDomain(description);
                        domain.addSubDomain(subDomain);
                    } else {
                        if (!subDomain.getSubDomain().equals(description.subDomain().name())) {
                            throw new RuntimeException("SubDomain names different for same order " + description.subDomain().order()
                                    + ": Found: " + subDomain.getSubDomain() + " - Given: " + description.subDomain().name());
                        }
                    }
                    return subDomain;

                }

            }
        }
        return domain;
    }

    private DocSubDomain createDocSubDomain(DomainDescription description) {
        DocSubDomain subDomain = new DocSubDomain();
        subDomain.setDomain(description.name());
        subDomain.setDomainShortDescription(description.shortDescription());
        subDomain.setOrder(description.subDomain().order());
        subDomain.setSubDomain(description.subDomain().name());
        subDomain.setSubDomainShortDescription(description.subDomain().shortDescription());
        subDomain.setDescription(description.subDomain().description());
        subDomain.setExternalDocumentation(description.subDomain().externalDocumentation().location());
        subDomain.setExternalInsertPosition(description.subDomain().externalDocumentation().postion());
        return subDomain;
    }

    private String getHttpMethod(Method method) {
        return StringUtils.join(method.getAnnotation(RequestMapping.class).method(), ",");
    }

    private String getResponseClass(Method method) {
        Method documentedMethod = getMethodFromDocumentation(method);
        if (documentedMethod.isAnnotationPresent(ResponseDescription.class)) {
            ResponseDescription responseDescription = documentedMethod.getAnnotation(ResponseDescription.class);
            if (responseDescription.type() != void.class) {
                dataTypeParser.addDataType(responseDescription.type());
                return removeEnd(responseDescription.type().getSimpleName(), config.getDataTypeSuffix());
            }
        }
        if (method.isAnnotationPresent(ResponseBody.class)) {
            if (Inspector.isListSetOrArray(method.getReturnType())) {
                Class<?> genericClass = Inspector.getGenericClassOfList(method.getReturnType(), method.getGenericReturnType());
                dataTypeParser.addDataType(genericClass);
                return "List[" + removeEnd(genericClass.getSimpleName(), config.getDataTypeSuffix()) + "]";
            }
            if (!method.getReturnType().getName().startsWith("java.lang")) {
                dataTypeParser.addDataType(method.getReturnType());
            }
            return removeEnd(method.getReturnType().getSimpleName(), config.getDataTypeSuffix());
        } else {
            return "";
        }
    }

    private List<DocParameter> getParametersOfMethod(Method method, String apiPath) {
        System.out.println("Inspecting method " + method.getName() + " with path " + apiPath);
        Pattern p = Pattern.compile("\\{.+?\\}");
        Matcher pathMatcher = p.matcher(apiPath);
        List<DocParameter> parameters = new ArrayList<>();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < parameterAnnotations.length; i++) {
            if (parameterAnnotations[i].length > 0) {
                ParameterDescription parameterDescription = getParameterDescription(method);
                DocParameter docParameter = new DocParameter();
                docParameter.setRequired(true);
                Class<?> parameterType = parameterTypes[i];
                Annotation annotation = parameterAnnotations[i][0];
                if (annotation.annotationType() == PathVariable.class) {
                    addPathVariableParametersToDocParameter(apiPath, pathMatcher, parameterDescription, docParameter, parameterType);
                } else if (annotation.annotationType() == RequestBody.class) {
                    addRequestBodyParametersToDocParameter(parameterDescription, docParameter, parameterType);
                } else if (annotation.annotationType() == RequestParam.class) {
                    addQueryParametersToParameterDoc(parameterDescription, docParameter, parameterType, annotation);
                }
                if (!docParameter.isIgnore()) {
                    parameters.add(docParameter);
                    if (!docParameter.getDataTypeClass().getName().startsWith("java.lang") || !docParameter.isList()) {
                        dataTypeParser.addDataType(docParameter.getDataTypeClass());
                    }
                } else {
                    System.out.println("Ignoring parameter " + docParameter.getReferenceName() + " - " + docParameter.getParamType());
                }
            }
        }
        return parameters;
    }

    private ParameterDescription getParameterDescription(Method method) {
        Method documentedMethod = getMethodFromDocumentation(method);
        if (documentedMethod.isAnnotationPresent(ParameterDescription.class)) {
            return documentedMethod.getAnnotation(ParameterDescription.class);
        }
        throw new UndocumentedMethodException("No parameter description found at method " + method.getName());
    }

    private void addPathVariableParametersToDocParameter(String apiPath, Matcher pathMatcher, ParameterDescription parameterDescription,
                                                         DocParameter docParameter, Class<?> parameterType) {
        docParameter.setParamType("path");
        docParameter.setDataType("string");
        if (pathMatcher.find()) {
            String name = apiPath.substring(pathMatcher.start() + 1, pathMatcher.end() - 1);
            docParameter.setName(name);
            docParameter.setReferenceName(name);
        } else {
            System.out.println("Could not find name");
        }
        Parameter parameter = getParameter(parameterDescription, docParameter.getReferenceName());
        docParameter.setDataTypeClass(getDataTypeClass(parameter, parameterType));
        docParameter.setDescription(parameter.description());
        docParameter.setIgnore(parameter.ignore());
    }

    private void addRequestBodyParametersToDocParameter(ParameterDescription parameterDescription, DocParameter docParameter,
                                                        Class<?> parameterType) {
        docParameter.setParamType("body");
        Parameter parameter = getParameter(parameterDescription, "requestBody");
        Class<?> dataTypeClass = getDataTypeClass(parameter, parameterType);
        String dataTypeName = removeEnd(removeEnd(dataTypeClass.getSimpleName(), "[]"), config.getDataTypeSuffix());
        docParameter.setName(dataTypeName.toLowerCase());
        docParameter.setReferenceName(dataTypeName.toLowerCase() + "-data");
        docParameter.setDataTypeClass(dataTypeClass);
        docParameter.setDescription(parameter.description());
        docParameter.setIgnore(parameter.ignore());
        if (parameterType.isArray()) {
            docParameter.setReferenceName(dataTypeName.toLowerCase() + "-list-data");
            dataTypeParser.addListDataTypeStubAndAddRealDataType(docParameter, parameterType, dataTypeName);
        }

    }


    private void addQueryParametersToParameterDoc(ParameterDescription parameterDescription, DocParameter docParameter,
                                                  Class<?> parameterType, Annotation annotation) {
        RequestParam requestParam = (RequestParam) annotation;
        docParameter.setParamType("query");
        docParameter.setName(requestParam.value());
        docParameter.setReferenceName(requestParam.value() + (requestParam.required() ? "-required" : "-optional") + "-query");
        docParameter.setRequired(requestParam.required());
        Parameter parameter = getParameter(parameterDescription, requestParam.value());
        docParameter.setDataTypeClass(getDataTypeClass(parameter, parameterType));
        docParameter.setIgnore(parameter.ignore());
        docParameter.setDescription(parameter.description());
    }

    private Parameter getParameter(ParameterDescription parameterDescription, String name) {
        if (StringUtils.isBlank(name)) {
            throw new RuntimeException(
                    "No name found in @RequestParam annotation please check if all @RequestParams are annotated with value = \"name\"");
        }
        for (Parameter parameter : parameterDescription.value()) {
            if (parameter.name().equals(name)) {
                return parameter;
            }
        }
        throw new RuntimeException("Could not find parameter-description for parameter " + name);
    }

    private Class<?> getDataTypeClass(Parameter parameter, Class<?> clazz) {
        if (parameter != null && !parameter.type().equals(void.class)) {
            return parameter.type();
        }
        return clazz;
    }


    public Map<Integer, DocDomain> getDomains() {
        return domains;
    }

    public Set<DocError> getListOfCommonErrors() {
        return listOfCommonErrors;
    }

}
