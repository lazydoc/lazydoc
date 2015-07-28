package org.lazydoc.reporter;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

public class DocumentationReporter {

    private static final Logger log = LogManager.getLogger(DocumentationReporter.class);


    private Map<Class<?>, ControllerDocumentationProgress> controllersProgress = new HashMap<>();
    private Map<Class<?>, ModelDocumentationProgress> modelsProgress = new HashMap<>();

    private int controllers = 0;
    private int ignoredControllers = 0;
    private int undocumentedControllers = 0;
    private int documentedMethods = 0;
    private int ignoredMethods = 0;
    private int undocumentedMethods = 0;
    private int documentedErrorHandlers = 0;
    private int ignoredErrorHandlers = 0;
    private int undocumentedErrorHandlers = 0;
    private int models = 0;

    public void addUndocumentedController(Class<?> controller) {
        getControllerDocumentationProgress(controller).setUndocumented(true);
        undocumentedControllers++;
    }

    public void addIgnoredController(Class<?> controller, String reason) {
        ControllerDocumentationProgress controllerDocumentationProgress = getControllerDocumentationProgress(controller);
        controllerDocumentationProgress.setIgnored(true);
        controllerDocumentationProgress.setIgnoreReason(reason);
        ignoredControllers++;
    }

    public void addUndocumentedErrorHandler(Class<?> controller, String errorHandler) {
        getControllerDocumentationProgress(controller).getUndocumentedErrorHandlers().add(errorHandler);
        undocumentedErrorHandlers++;
    }

    public void addIgnoredErrorHandler(Class<?> controller, String errorHandler) {
        getControllerDocumentationProgress(controller).getIgnoredErrorHandlers().add(errorHandler);
        ignoredErrorHandlers++;
    }

    public void addDocumentedErrorHandler(Class<?> controller, String errorHandler) {
        getControllerDocumentationProgress(controller).getDocumentedErrorHandlers().add(errorHandler);
        documentedErrorHandlers++;
    }

    public void addDocumentedMethod(Class<?> controller, String method) {
        getControllerDocumentationProgress(controller).getDocumentedMethods().add(method);
        documentedMethods++;
    }

    public void addIgnoredMethod(Class<?> controller, String method) {
        getControllerDocumentationProgress(controller).getIgnoredMethods().add(method);
        ignoredMethods++;
    }

    public void addUndocumentedMethod(Class<?> controller, String method) {
        getControllerDocumentationProgress(controller).getUndocumentedMethods().add(method);
        undocumentedMethods++;
    }

    public void addDocumentedField(Class<?> model, String fieldName) {
        getModelDocumentationProgress(model).getDocumentedFields().add(fieldName);

    }

    public void addUndocumentedField(Class<?> model, String fieldName) {
        getModelDocumentationProgress(model).getUndocumentedFields().add(fieldName);
    }

    public void addIgnoredField(Class<?> model, String fieldName) {
        getModelDocumentationProgress(model).getIgnoredFields().add(fieldName);
    }

    public int getUndocumentedCount() {
        int undocumented = 0;
        for(ControllerDocumentationProgress controller : controllersProgress.values()) {
            undocumented += controller.getUndocumentedErrorHandlers().size() + controller.getUndocumentedMethods().size();
        }
        for(ModelDocumentationProgress model : modelsProgress.values()) {
            undocumented += model.getUndocumentedFields().size();
        }
        return undocumented;
    }


    private ControllerDocumentationProgress getControllerDocumentationProgress(Class<?> controller) {
        ControllerDocumentationProgress controllerProgress = controllersProgress.get(controller);
        if (controllerProgress == null) {
            controllerProgress = new ControllerDocumentationProgress(controller);
            controllersProgress.put(controller, controllerProgress);
            controllers++;
        }
        return controllerProgress;
    }

    private ModelDocumentationProgress getModelDocumentationProgress(Class<?> model) {
        ModelDocumentationProgress modelDocumentationProgress = modelsProgress.get(model);
        if (modelDocumentationProgress == null) {
            modelDocumentationProgress = new ModelDocumentationProgress(model);
            modelsProgress.put(model, modelDocumentationProgress);
            models++;
        }
        return modelDocumentationProgress;
    }

    public void printSummaryReport() {
        int undocumentedFields = 0;
        int ignoredFields = 0;
        int documentedFields = 0;

        for(ModelDocumentationProgress model: modelsProgress.values()) {
            undocumentedFields += model.getUndocumentedFields().size();
            documentedFields += model.getDocumentedFields().size();
            ignoredFields += model.getIgnoredFields().size();
        }

        Double documented = (double) (documentedMethods + documentedErrorHandlers + documentedFields);
        Double undocumented = (double) (undocumentedMethods + undocumentedErrorHandlers + undocumentedFields);
        Double ignored = (double) (ignoredMethods + ignoredErrorHandlers + ignoredFields);
        double documentationCoverage = documented * 100.0 / (documented + undocumented + ignored);
        double documentationCoverageWithoutIgnored = documented * 100.0 / (documented + undocumented);
        log.info("-----------------------------------------------------------------------------------------------------------");
        log.info("");
        log.info(" DOCUMENTATION SUMMARY REPORT");
        log.info("");
        log.info("-----------------------------------------------------------------------------------------------------------");
        log.info("Overall controllers: " + controllers);
        log.info("Documented controllers: " + (controllers-undocumentedControllers-ignoredControllers));
        log.info("Undocumented controllers: " + undocumentedControllers);
        log.info("Ignored controllers: " + ignoredControllers);
        log.info("Documented methods: " + documentedMethods);
        log.info("Documented error handlers: " + documentedErrorHandlers);
        log.info("Undocumented methods: " + undocumentedMethods);
        log.info("Undocumented error handlers: " + undocumentedErrorHandlers);
        log.info("Ignored methods: " + ignoredMethods);
        log.info("Ignored error handlers: " + ignoredErrorHandlers);
        log.info("Models: " + models);
        log.info("Documented fields: " + documentedFields);
        log.info("Undocumented fields: " + undocumentedFields);
        log.info("Ignored fields: " + ignoredFields);
        log.info("-----------------------------------------------------------------------------------------------------------");
        log.info("Documentation coverage: " + (Double.isNaN(documentationCoverage) ? "0.00" : new DecimalFormat("0.00").format(documentationCoverage)) + "%");
        log.info("Documentation coverage without ignored: " + (Double.isNaN(documentationCoverageWithoutIgnored) ? "0.00" : ""+ new DecimalFormat("0.00").format(documentationCoverageWithoutIgnored)) + "%");
        log.info("-----------------------------------------------------------------------------------------------------------");

    }

    public void printProgressReport() {
        log.info("-----------------------------------------------------------------------------------------------------------");
        log.info("");
        log.info(" DOCUMENTATION PROGRESS REPORT");
        log.info("");
        log.info("-----------------------------------------------------------------------------------------------------------");
        log.info("#############################");
        log.info(" Undocumented Controllers:");
        log.info("#############################");
        for (ControllerDocumentationProgress controllerDocumentationProgress : controllersProgress.values()) {
            if (controllerDocumentationProgress.isUndocumented()) {
                printMethodsOfController(controllerDocumentationProgress);
            }
        }
        log.info("#############################");
        log.info(" Ignored Controllers:");
        log.info("#############################");
        for (ControllerDocumentationProgress controllerDocumentationProgress : controllersProgress.values()) {
            if (controllerDocumentationProgress.isIgnored()) {
                log.info("Reason to ignore controller: " + controllerDocumentationProgress.getIgnoreReason());
                printMethodsOfController(controllerDocumentationProgress);
            }
        }
        log.info("#############################");
        log.info(" Documented Controllers:");
        log.info("#############################");
        for (ControllerDocumentationProgress controllerDocumentationProgress : controllersProgress.values()) {
            if (!controllerDocumentationProgress.isIgnored() && !controllerDocumentationProgress.isUndocumented()) {
                printMethodsOfController(controllerDocumentationProgress);

            }
        }
        log.info("-----------------------------------------------------------------------------------------------------------");
        for (ModelDocumentationProgress modelDocumentationProgress : modelsProgress.values()) {
            log.info("Model " + modelDocumentationProgress.getModelClass().getSimpleName());
            if (modelDocumentationProgress.getDocumentedFields().size() > 0) {
                log.info("**** Documented fields ****");
                for (String field : modelDocumentationProgress.getDocumentedFields()) {
                    log.info(field);
                }
            }
            if (modelDocumentationProgress.getIgnoredFields().size() > 0) {
                log.info("**** Ignored fields ****");
                for (String field : modelDocumentationProgress.getIgnoredFields()) {
                    log.info(field);
                }
            }
            if (modelDocumentationProgress.getUndocumentedFields().size() > 0) {
                log.info("**** Undocumented fields ****");
                for (String field : modelDocumentationProgress.getUndocumentedFields()) {
                    log.info(field);
                }
            }
            log.info("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -");
        }
    }

    private void printMethodsOfController(ControllerDocumentationProgress controllerDocumentationProgress) {
        log.info("Controller " + controllerDocumentationProgress.getController().getSimpleName());
        if (controllerDocumentationProgress.getUndocumentedMethods().size() > 0) {
            log.info("**** Undocumented methods ****");
            for (String method : controllerDocumentationProgress.getUndocumentedMethods()) {
                log.info(method);
            }
        }
        if (controllerDocumentationProgress.getIgnoredMethods().size() > 0) {
            log.info("**** Ignored methods ****");
            for (String method : controllerDocumentationProgress.getIgnoredMethods()) {
                log.info(method);
            }
        }
        if (controllerDocumentationProgress.getIgnoredMethods().size() > 0) {
            log.info("**** Undocumented Errorhandlers ****");
            for (String errorHandler : controllerDocumentationProgress.getUndocumentedErrorHandlers()) {
                log.info(errorHandler);
            }
        }
        log.info("");
    }
 
    public void printProgressTable() {
        log.info("-----------------------------------------------------------------------------------------------------------");
        log.info("");
        log.info(" DOCUMENTATION PROGRESS TABLE");
        log.info("");
        log.info("-----------------------------------------------------------------------------------------------------------");
        log.info("| CONTROLLER                                                  |  DOCUMENTED  | UNDOCUMENTED |   IGNORED   |");
        log.info("-----------------------------------------------------------------------------------------------------------");
        for (ControllerDocumentationProgress progress : controllersProgress.values()) {
            log.info("| " + StringUtils.rightPad(progress.getController().getSimpleName(), 60)
            		+ "| "+StringUtils.rightPad(""+(progress.getDocumentedMethods().size()+progress.getDocumentedErrorHandlers().size()), 13)
            		+ "| "+StringUtils.rightPad(""+(progress.getUndocumentedMethods().size()+progress.getUndocumentedErrorHandlers().size()), 13)
            		+ "| "+StringUtils.rightPad(""+(progress.getIgnoredMethods().size()+progress.getIgnoredErrorHandlers().size()), 12)
            		+ "|");
        }
        log.info("-----------------------------------------------------------------------------------------------------------");
        log.info("| MODEL                                                       |  DOCUMENTED  | UNDOCUMENTED |   IGNORED   |");
        log.info("-----------------------------------------------------------------------------------------------------------");
        for (ModelDocumentationProgress progress : modelsProgress.values()) {
            log.info("| " + StringUtils.rightPad(progress.getModelClass().getSimpleName(), 60)
            		+ "| "+StringUtils.rightPad(""+progress.getDocumentedFields().size(), 13)
            		+ "| "+StringUtils.rightPad(""+progress.getUndocumentedFields().size(), 13)
            		+ "| "+StringUtils.rightPad(""+progress.getIgnoredFields().size(), 12)
            		+ "|");
        }
        log.info("-----------------------------------------------------------------------------------------------------------");
    }

	public void printOverallProgressReport() {
		printProgressReport();
		printProgressTable();
		printSummaryReport();
	}


}
