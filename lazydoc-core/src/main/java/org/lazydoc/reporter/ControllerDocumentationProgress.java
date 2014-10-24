package org.lazydoc.reporter;

import java.util.Set;
import java.util.TreeSet;

public class ControllerDocumentationProgress {

    private Class<?> controller;
    private boolean undocumented;
    private boolean ignored;
    private String ignoreReason;
    private Set<String> documentedMethods = new TreeSet<>();
    private Set<String> undocumentedMethods = new TreeSet<>();
    private Set<String> ignoredMethods = new TreeSet<>();
    private Set<String> documentedErrorHandlers = new TreeSet<>();
    private Set<String> undocumentedErrorHandlers = new TreeSet<>();
    private Set<String> ignoredErrorHandlers = new TreeSet<>();

    public ControllerDocumentationProgress(Class<?> controller) {
        this.controller = controller;
    }

    public Class<?> getController() {
        return controller;
    }


    public boolean isUndocumented() {
        return undocumented;
    }

    public void setUndocumented(boolean undocumented) {
        this.undocumented = undocumented;
    }

    public boolean isIgnored() {
        return ignored;
    }

    public void setIgnored(boolean ignored) {
        this.ignored = ignored;
    }

    public String getIgnoreReason() {
        return ignoreReason;
    }

    public void setIgnoreReason(String ignoreReason) {
        this.ignoreReason = ignoreReason;
    }


    public Set<String> getDocumentedMethods() {
        return documentedMethods;
    }

    public Set<String> getIgnoredMethods() {
        return ignoredMethods;
    }

    public Set<String> getUndocumentedMethods() {
        return undocumentedMethods;
    }

    public Set<String> getDocumentedErrorHandlers() {
        return documentedErrorHandlers;
    }

    public Set<String> getUndocumentedErrorHandlers() {
        return undocumentedErrorHandlers;
    }

    public Set<String> getIgnoredErrorHandlers() {
        return ignoredErrorHandlers;
    }
}
