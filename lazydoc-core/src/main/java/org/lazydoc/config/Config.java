package org.lazydoc.config;

import java.util.ArrayList;
import java.util.List;

public class Config {
	
	private String customAnnotationToBeIgnored;
	private String dataTypeSuffix;
	private String documentationSuffix;
	private String packageToSearchForControllers;
	private String exceptionHandlerInvoker;
	private String baseDTOClassname;
	private String swaggerBasePath;
    private String abstractControllerClassForCommonExceptionHandlers;
    private String stopInspectionAtAbstractController;
    private String instanceControllerClassForCommonExceptionHandlers;
    private String controllerClassToStopErrorInspection;
    private List<PrinterConfig> printerConfigs = new ArrayList<>();

    public String getCustomAnnotationToBeIgnored() {
		return customAnnotationToBeIgnored;
	}
	public void setCustomAnnotationToBeIgnored(String customAnnotationToBeIgnored) {
		this.customAnnotationToBeIgnored = customAnnotationToBeIgnored;
	}
	public String getDataTypeSuffix() {
		return dataTypeSuffix;
	}
	public void setDataTypeSuffix(String dataTypeSuffix) {
		this.dataTypeSuffix = dataTypeSuffix;
	}
	public String getDocumentationSuffix() {
		return documentationSuffix;
	}
	public void setDocumentationSuffix(String documentationSuffix) {
		this.documentationSuffix = documentationSuffix;
	}
	public String getPackageToSearchForControllers() {
		return packageToSearchForControllers;
	}
	public void setPackageToSearchForControllers(
			String packageToSearchForControllers) {
		this.packageToSearchForControllers = packageToSearchForControllers;
	}
	
	public String getExceptionHandlerInvoker() {
		return exceptionHandlerInvoker;
	}
	public void setExceptionHandlerInvoker(String exceptionHandlerInvoker) {
		this.exceptionHandlerInvoker = exceptionHandlerInvoker;
	}
	
	public String getBaseDTOClassname() {
		return baseDTOClassname;
	}
	
	public void setBaseDTOClassname(String baseDTOClassname) {
		this.baseDTOClassname = baseDTOClassname;
	}
	
	public String getSwaggerBasePath() {
		return swaggerBasePath;
	}
	
	public void setSwaggerBasePath(String swaggerBasePath) {
		this.swaggerBasePath = swaggerBasePath;
	}

    public String getAbstractControllerClassForCommonExceptionHandlers() {
        return abstractControllerClassForCommonExceptionHandlers;
    }

    public void setAbstractControllerClassForCommonExceptionHandlers(String abstractControllerClassForCommonExceptionHandlers) {
        this.abstractControllerClassForCommonExceptionHandlers = abstractControllerClassForCommonExceptionHandlers;
    }

    public String getStopInspectionAtAbstractController() {
        return stopInspectionAtAbstractController;
    }

    public void setStopInspectionAtAbstractController(String stopInspectionAtAbstractController) {
        this.stopInspectionAtAbstractController = stopInspectionAtAbstractController;
    }

    public List<PrinterConfig> getPrinterConfigs() {
        return printerConfigs;
    }

    public String getInstanceControllerClassForCommonExceptionHandlers() {
        return instanceControllerClassForCommonExceptionHandlers;
    }

    public void setInstanceControllerClassForCommonExceptionHandlers(String instanceControllerClassForCommonExceptionHandlers) {
        this.instanceControllerClassForCommonExceptionHandlers = instanceControllerClassForCommonExceptionHandlers;
    }

    public String getControllerClassToStopErrorInspection() {
        return controllerClassToStopErrorInspection;
    }

    public void setControllerClassToStopErrorInspection(String controllerClassToStopErrorInspection) {
        this.controllerClassToStopErrorInspection = controllerClassToStopErrorInspection;
    }
}
