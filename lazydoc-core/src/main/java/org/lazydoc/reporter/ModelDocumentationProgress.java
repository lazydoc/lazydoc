package org.lazydoc.reporter;

import java.util.Set;
import java.util.TreeSet;

public class ModelDocumentationProgress {

    private Class<?> modelClass;
    private Set<String> documentedFields = new TreeSet<>();
    private Set<String> undocumentedFields = new TreeSet<>();
    private Set<String> ignoredFields = new TreeSet<>();

    public ModelDocumentationProgress(Class<?> modelClass) {
        this.modelClass = modelClass;
    }

    public Class<?> getModelClass() {
        return modelClass;
    }

    public Set<String> getDocumentedFields() {
        return documentedFields;
    }

    public Set<String> getUndocumentedFields() {
        return undocumentedFields;
    }

    public Set<String> getIgnoredFields() {
        return ignoredFields;
    }
}
