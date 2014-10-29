package org.lazydoc.reporter;

import org.apache.commons.lang3.StringUtils;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

public class DocumentationReporter {

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
        System.out.println("-----------------------------------------------------------------------------------------------------------");
        System.out.println("");
        System.out.println(" DOCUMENTATION SUMMARY REPORT");
        System.out.println("");
        System.out.println("-----------------------------------------------------------------------------------------------------------");
        System.out.println("Overall controllers: " + controllers);
        System.out.println("Undocumented controllers: " + undocumentedControllers);
        System.out.println("Ignored controllers: " + ignoredControllers);
        System.out.println("Documented methods: " + documentedMethods);
        System.out.println("Documented error handlers: " + documentedErrorHandlers);
        System.out.println("Undocumented methods: " + undocumentedMethods);
        System.out.println("Undocumented error handlers: " + undocumentedErrorHandlers);
        System.out.println("Ignored methods: " + ignoredMethods);
        System.out.println("Ignored error handlers: " + ignoredErrorHandlers);
        System.out.println("Models: " + models);
        System.out.println("Documented fields: " + documentedFields);
        System.out.println("Undocumented fields: " + undocumentedFields);
        System.out.println("Ignored fields: " + ignoredFields);
        System.out.println("-----------------------------------------------------------------------------------------------------------");
        System.out.println("Documentation coverage: " + new DecimalFormat("0.00").format(documentationCoverage) + "%");
        System.out.println("Documentation coverage without ignored: " + new DecimalFormat("0.00").format(documentationCoverageWithoutIgnored) + "%");
        System.out.println("-----------------------------------------------------------------------------------------------------------");

    }

    public void printProgressReport() {
        System.out.println("-----------------------------------------------------------------------------------------------------------");
        System.out.println("");
        System.out.println(" DOCUMENTATION PROGRESS REPORT");
        System.out.println("");
        System.out.println("-----------------------------------------------------------------------------------------------------------");
        System.out.println("#############################");
        System.out.println(" Undocumented Controllers:");
        System.out.println("#############################");
        for (ControllerDocumentationProgress controllerDocumentationProgress : controllersProgress.values()) {
            if (controllerDocumentationProgress.isUndocumented()) {
                printMethodsOfController(controllerDocumentationProgress);
            }
        }
        System.out.println("#############################");
        System.out.println(" Ignored Controllers:");
        System.out.println("#############################");
        for (ControllerDocumentationProgress controllerDocumentationProgress : controllersProgress.values()) {
            if (controllerDocumentationProgress.isIgnored()) {
                System.out.println("Reason to ignore controller: " + controllerDocumentationProgress.getIgnoreReason());
                printMethodsOfController(controllerDocumentationProgress);
            }
        }
        System.out.println("#############################");
        System.out.println(" Documented Controllers:");
        System.out.println("#############################");
        for (ControllerDocumentationProgress controllerDocumentationProgress : controllersProgress.values()) {
            System.out.println("Controller " + controllerDocumentationProgress.getController().getSimpleName());
            if (!controllerDocumentationProgress.isIgnored() && !controllerDocumentationProgress.isUndocumented()) {
                printMethodsOfController(controllerDocumentationProgress);

            }
        }
        System.out.println("-----------------------------------------------------------------------------------------------------------");
        for (ModelDocumentationProgress modelDocumentationProgress : modelsProgress.values()) {
            System.out.println("Model " + modelDocumentationProgress.getModelClass().getSimpleName()+"\n");
            if (modelDocumentationProgress.getDocumentedFields().size() > 0) {
                System.out.println("**** Documented fields ****");
                for (String field : modelDocumentationProgress.getDocumentedFields()) {
                    System.out.println(field);
                }
            }
            if (modelDocumentationProgress.getIgnoredFields().size() > 0) {
                System.out.println("**** Ignored fields ****");
                for (String field : modelDocumentationProgress.getIgnoredFields()) {
                    System.out.println(field);
                }
            }
            if (modelDocumentationProgress.getUndocumentedFields().size() > 0) {
                System.out.println("**** Undocumented fields ****");
                for (String field : modelDocumentationProgress.getUndocumentedFields()) {
                    System.out.println(field);
                }
            }
            System.out.println("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -");
        }
    }

    private void printMethodsOfController(ControllerDocumentationProgress controllerDocumentationProgress) {
        System.out.println("Controller " + controllerDocumentationProgress.getController().getSimpleName()+"\n");
        if (controllerDocumentationProgress.getUndocumentedMethods().size() > 0) {
            System.out.println("**** Undocumented methods ****");
            for (String method : controllerDocumentationProgress.getUndocumentedMethods()) {
                System.out.println(method);
            }
        }
        if (controllerDocumentationProgress.getIgnoredMethods().size() > 0) {
            System.out.println("**** Ignored methods ****");
            for (String method : controllerDocumentationProgress.getIgnoredMethods()) {
                System.out.println(method);
            }
        }
        if (controllerDocumentationProgress.getIgnoredMethods().size() > 0) {
            System.out.println("**** Undocumented Errorhandlers ****");
            for (String errorHandler : controllerDocumentationProgress.getUndocumentedErrorHandlers()) {
                System.out.println(errorHandler);
            }
        }
        System.out.println("");
    }
 
    public void printProgressTable() {
        System.out.println("-----------------------------------------------------------------------------------------------------------");
        System.out.println("");
        System.out.println(" DOCUMENTATION PROGRESS TABLE");
        System.out.println("");
        System.out.println("-----------------------------------------------------------------------------------------------------------");
        System.out.println("| CONTROLLER                                                  |  DOCUMENTED  | UNDOCUMENTED |   IGNORED   |");
        System.out.println("-----------------------------------------------------------------------------------------------------------");
        for (ControllerDocumentationProgress progress : controllersProgress.values()) {
            System.out.println("| " + StringUtils.rightPad(progress.getController().getSimpleName(), 60)
            		+ "| "+StringUtils.rightPad(""+(progress.getDocumentedMethods().size()+progress.getDocumentedErrorHandlers().size()), 13)
            		+ "| "+StringUtils.rightPad(""+(progress.getUndocumentedMethods().size()+progress.getUndocumentedErrorHandlers().size()), 13)
            		+ "| "+StringUtils.rightPad(""+(progress.getIgnoredMethods().size()+progress.getIgnoredErrorHandlers().size()), 12)
            		+ "|");
        }
        System.out.println("-----------------------------------------------------------------------------------------------------------");
        System.out.println("| MODEL                                                       |  DOCUMENTED  | UNDOCUMENTED |   IGNORED   |");
        System.out.println("-----------------------------------------------------------------------------------------------------------");
        for (ModelDocumentationProgress progress : modelsProgress.values()) {
            System.out.println("| " + StringUtils.rightPad(progress.getModelClass().getSimpleName(), 60)
            		+ "| "+StringUtils.rightPad(""+progress.getDocumentedFields().size(), 13)
            		+ "| "+StringUtils.rightPad(""+progress.getUndocumentedFields().size(), 13)
            		+ "| "+StringUtils.rightPad(""+progress.getIgnoredFields().size(), 12)
            		+ "|");
        }
        System.out.println("-----------------------------------------------------------------------------------------------------------");
    }

	public void printOverallProgressReport() {
		printProgressReport();
		printProgressTable();
		printSummaryReport();
	}


}
