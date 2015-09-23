package org.lazydoc.parser.spring;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

    private static final Logger log = LogManager.getLogger(SpringParser.class);

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
        if (StringUtils.isBlank(packageToSearchForControllers)) {
            throw new RuntimeException("Please provide package to search for controllers in configuration");
        }
        log.debug("Looking up on package " + packageToSearchForControllers);
        Set<Class<?>> controllerSet = new Reflections(packageToSearchForControllers).getTypesAnnotatedWith(Controller.class);
        log.debug("Spring controllers found : " + controllerSet.size());
        Set<Class<?>> restcontrollersSet = new Reflections(packageToSearchForControllers).getTypesAnnotatedWith(RestController.class);
        log.debug("Spring restcontrollers found : " + restcontrollersSet.size());
        controllerSet.addAll(restcontrollersSet);
        log.debug("Found Controllers: " + StringUtils.join(controllerSet, ", "));
        return controllerSet;
    }

    private void extractControllerDetails(SortedSet<String> requestMappings, Class<?> controller) {
        log.debug("Inspecting controller " + controller.getSimpleName());
        Class<?> documentation = getDocumentation(controller);
        if (excludeFromDocumentation(controller) || hasNoDocumentation(documentation, controller)) {
            return;
        }
        if (controllerIsNotAbstract(controller) && isControllerAnnotationPresent(controller)) {
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
                        domain.setDeprecated(controller.isAnnotationPresent(Deprecated.class));
                        addOperation(method, path, domain);
                        reporter.addDocumentedMethod(controller, method.toString());
                    }
                } catch (UndocumentedMethodException ex) {
                    log.debug("UNDOCUMENTED METHOD: " + ex.getMessage());
                    reporter.addUndocumentedMethod(controller, method.toString());
                }

            }
        }
    }

    private Set<DocError> getErrorList(ErrorDescription[] errorDescriptions) {
        Set<DocError> errorList = new TreeSet<>();
        if(errorDescriptions.length > 0) {
            for (ErrorDescription errorDescription : errorDescriptions) {
                errorList.add(new DocError(errorDescription.statusCode(), errorDescription.detail(), errorDescription.description()));
            }
            log.info("Found error list: "+StringUtils.join(errorList), ",");
        }
        return errorList;
    }

    private Set<DocError> getListOfPossibleErrors(Class<?> controller) {
        return getListOfPossibleErrors(controller, null);
    }

    private Set<DocError> getListOfPossibleErrors(Class<?> controller, List<Class<?>> exceptionList) {
        Class<?> stopInspectionAtControllerClass = null;
        if (StringUtils.isNotBlank(config.getControllerClassToStopErrorInspection())) {
            stopInspectionAtControllerClass = getClassByName(config.getControllerClassToStopErrorInspection());
        }
        return getListOfPossibleErrors(controller, stopInspectionAtControllerClass, InstanceCreator.createInstanceOf(controller), new TreeSet<DocError>(), exceptionList);
    }


    private void addCommonListOfPossibleErrors() {
        if (StringUtils.isNotBlank(config.getAbstractControllerClassForCommonExceptionHandlers()) && StringUtils.isNotBlank(config.getInstanceControllerClassForCommonExceptionHandlers())) {
            Class<?> abstractControllerClass = getClassByName(config.getAbstractControllerClassForCommonExceptionHandlers());
            Class<?> instanceControllerClass = getClassByName(config.getInstanceControllerClassForCommonExceptionHandlers());
            listOfCommonErrors.addAll(getListOfPossibleErrors(abstractControllerClass, null, InstanceCreator.createInstanceOf(instanceControllerClass), new TreeSet<DocError>(), null));
        }
    }

    private Set<DocError> getListOfPossibleErrors(Class<?> controller, Class<?> stopAtAbstractController, Object controllerInstance,
                                                  Set<DocError> result, List<Class<?>> exceptionList) {
        if (controller.equals(Object.class) || (stopAtAbstractController != null && controller.equals(stopAtAbstractController))) {
            return result;
        } else {
            log.debug("Inspecting error handler in controller class " + controller.getSimpleName());
            getListOfPossibleErrors(controller.getSuperclass(), stopAtAbstractController, controllerInstance, result, exceptionList);
        }
        for (Method method : controller.getDeclaredMethods()) {
            if (method.isAnnotationPresent(ExceptionHandler.class)) {
                ExceptionHandler exceptionHandler = method.getAnnotation(ExceptionHandler.class);
                if (isExceptionListProvided(exceptionList) && !isExceptionHandlerForProvidedExceptions(exceptionList, exceptionHandler)) {
                    log.debug("No matching exception of exception handler in provided list found");
                    continue;
                }
                log.debug("Inspecting exception handler " + method.getName());
                HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
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
                    errorMessage = errorDescription.detail();
                    description = errorDescription.description();

                } else {
                    reporter.addUndocumentedErrorHandler(controller, method.toString());
                }
                if (StringUtils.isEmpty(errorMessage)) {
                    errorMessage = getErrorMessageFromExceptionHandler(method, controllerInstance, exceptionHandler);
                }
                result.add(new DocError(httpStatus.value(), errorMessage, description));
            }
        }

        return result;
    }

    private boolean isExceptionListProvided(List<Class<?>> exceptionList) {
        return exceptionList != null && !exceptionList.isEmpty();
    }

    private boolean isExceptionHandlerForProvidedExceptions(List<Class<?>> exceptionList, ExceptionHandler exceptionHandler) {
        log.debug("Exception handler exceptions " + StringUtils.join(exceptionHandler.value(), ","));
        log.debug("Exception list provided " + StringUtils.join(exceptionList, ","));
        Class<? extends Throwable>[] exceptions = exceptionHandler.value();
        for (Class<? extends Throwable> exception : exceptions) {
            for (Class<?> providedException : exceptionList) {
                if(exception.getName().equals(providedException.getName())) {
                    return true;
                }
            }
        }
        return false;
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
                log.debug("Could not find controller documentation in class " + controller.getName() + config.getDocumentationSuffix());
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
            log.debug("Ignoring method " + method.getName() + " (" + method.toString() + ") for documentation because of custom annotation " + config.getCustomAnnotationToBeIgnored());
            printMethodPathAndHttpMethod(generalRequestMapping, method);
            return true;
        }
        if (ignoreForDocumentation(method)) {
            if (methodHasRequestMapping(method)) {
                log.warn("Ignoring method " + method.getName() + " (" + method.toString() + ") for documentation");
                printMethodPathAndHttpMethod(generalRequestMapping, method);
                return true;
            }
        }
        return false;
    }

    private void printMethodPathAndHttpMethod(String generalRequestMapping, Method method) {
        if (methodHasRequestMapping(method)) {
            log.debug("Ignoring path " + getRequestMapping(generalRequestMapping, method) + " - " + getHttpMethod(method));
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
        if (StringUtils.isNotBlank(config.getDocumentationSuffix())) {
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
        return controller.isAnnotationPresent(Controller.class) || controller.isAnnotationPresent(RestController.class);
    }

    private boolean methodHasRequestMapping(Method method) {
        return method.isAnnotationPresent(RequestMapping.class);
    }

    private void addOperation(Method method, String path, DocDomain domain) {
        OperationDescription operationDescription = getOperationDescription(method);
        DocOperation operation = new DocOperation();
        operation.setHttpMethod(getHttpMethod(method));
        operation.setResponseStatus(getResponseStatus(method));
        operation.setOperationResponse(getOperationResponse(method));
        operation.setNickname(method.getName());
        operation.setPath(path);
        operation.setParameters(getParametersOfMethod(method, path));
        operation.setRole(getRoleFromMethod(method));
        operation.setStaticRequestSample(operationDescription.staticRequestSample());
        if (operationDescription != null) {
            if (StringUtils.isNotBlank(operationDescription.nickname())) {
                operation.setNickname(operationDescription.nickname());
            }
            operation.setShortDescription(operationDescription.shortDescription());
            operation.setDescription(operationDescription.description());
            operation.setNotes(operationDescription.notes());
            operation.setOrder(operationDescription.order());
            operation.setExternalDocumentations(getExternalDocumentations(operationDescription.externalDocumentation()));
        }
        operation.setDeprecated(method.isAnnotationPresent(Deprecated.class));
        operation.setErrorList(getErrorListForOperation(method));
        domain.getOperations().add(operation);
    }

    private Set<DocError> getErrorListForOperation(Method method) {
        Method methodFromDocumentation = getMethodFromDocumentation(method);
        if(methodFromDocumentation.isAnnotationPresent(ErrorDescriptions.class)) {
            return getErrorList(methodFromDocumentation.getAnnotation(ErrorDescriptions.class).value());
        }
        List<Class<?>> exceptionList = Arrays.asList(method.getExceptionTypes());
        if(exceptionList.isEmpty()) {
            return new TreeSet<>();
        }
        return getListOfPossibleErrors(method.getDeclaringClass(), exceptionList);
    }

    private OperationDescription getOperationDescription(Method method) {
        Method documentedMethod = getMethodFromDocumentation(method);
        if (documentedMethod.isAnnotationPresent(OperationDescription.class)) {
            return documentedMethod.getAnnotation(OperationDescription.class);
        }
        throw new UndocumentedMethodException("No Operation description found at method " + method.toString());
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
            domain.setExternalDocumentations(getExternalDocumentations(description.externalDocumentation()));
            domain.setErrorList(getErrorList(description.errors()));
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

    private List<DocExternalDocumentation> getExternalDocumentations(ExternalDocumentation[] documentations) {
        List<DocExternalDocumentation> externalDocumentations = new ArrayList<>();
        for (ExternalDocumentation documentation : documentations) {
            DocExternalDocumentation externalDocumentation = new DocExternalDocumentation();
            externalDocumentation.setLocation(documentation.location());
            externalDocumentation.setPostion(documentation.postion());
            externalDocumentations.add(externalDocumentation);
        }
        return externalDocumentations;
    }

    private DocSubDomain createDocSubDomain(DomainDescription description) {
        DocSubDomain subDomain = new DocSubDomain();
        subDomain.setDomain(description.name());
        subDomain.setDomainShortDescription(description.shortDescription());
        subDomain.setOrder(description.subDomain().order());
        subDomain.setSubDomain(description.subDomain().name());
        subDomain.setSubDomainShortDescription(description.subDomain().shortDescription());
        subDomain.setDescription(description.subDomain().description());
        subDomain.setExternalDocumentations(getExternalDocumentations(description.subDomain().externalDocumentation()));
        subDomain.setErrorList(getErrorList(description.subDomain().errors()));
        return subDomain;
    }

    private String getHttpMethod(Method method) {
        return StringUtils.join(method.getAnnotation(RequestMapping.class).method(), ",");
    }

    private String getResponseStatus(Method method) {
        if (method.isAnnotationPresent(ResponseStatus.class)) {
            ResponseStatus responseStatus = method.getAnnotation(ResponseStatus.class);
            return responseStatus.value().value() + " - " + StringUtils.defaultString(responseStatus.reason(), responseStatus.value().getReasonPhrase());
        }
        return HttpStatus.OK.value() + " - " + HttpStatus.OK.getReasonPhrase();
    }

    private DocOperationResponse getOperationResponse(Method method) {
        DocOperationResponse operationResponse = new DocOperationResponse();
        Method documentedMethod = getMethodFromDocumentation(method);
        if (documentedMethod.isAnnotationPresent(ResponseDescription.class)) {
            ResponseDescription responseDescription = documentedMethod.getAnnotation(ResponseDescription.class);
            if (responseDescription.type() != void.class) {
                dataTypeParser.addDataType(responseDescription.type());
                operationResponse.setResponseType(removeEnd(responseDescription.type().getSimpleName(), config.getDataTypeSuffix()));
                operationResponse.setResponseTypeClass(responseDescription.type());
                log.debug("GetOperationResponse - ResponseDescription " + operationResponse);
                return operationResponse;
            }
            operationResponse.setDescription(responseDescription.description());
            operationResponse.setStaticSample(responseDescription.staticSample());
            operationResponse.setSimpleTypeDescription(responseDescription.simpleTypeDescription());
        }
        if (methodHasResponseType(method)) {
            if (Inspector.isListSetOrArray(method.getReturnType())) {
                Class<?> genericClass = Inspector.getGenericClassOfList(method.getReturnType(), method.getGenericReturnType());
                dataTypeParser.addDataType(genericClass);
                operationResponse.setResponseType(removeEnd(genericClass.getSimpleName(), config.getDataTypeSuffix()));
                operationResponse.setResponseTypeClass(genericClass);
                operationResponse.setInList(true);
                log.debug("GetOperationResponse - RestController or ResponseBody annotation - list " + operationResponse);
                return operationResponse;
            } else {
                dataTypeParser.addDataType(method.getReturnType());
                operationResponse.setResponseType(removeEnd(method.getReturnType().getSimpleName(), config.getDataTypeSuffix()));
                operationResponse.setResponseTypeClass(method.getReturnType());
                log.debug("GetOperationResponse - RestController or ResponseBody annotation - no list" + operationResponse);
                return operationResponse;
            }
        }
        log.debug("GetOperationResponse - ResponseDescription " + operationResponse);
        return operationResponse;
    }

    private boolean methodHasResponseType(Method method) {
        return method.getDeclaringClass().isAnnotationPresent(RestController.class) || method.isAnnotationPresent(ResponseBody.class);
    }

    private List<DocParameter> getParametersOfMethod(Method method, String apiPath) {
        log.debug("Inspecting method " + method.getName() + " with path " + apiPath);
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
                    log.debug("Ignoring parameter " + docParameter.getReferenceName() + " - " + docParameter.getParamType());
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
            log.debug("Could not find name");
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
        Class<?> dataTypeClass = getDataTypeClass(parameter, parameterType);
        docParameter.setDataTypeClass(dataTypeClass);
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
