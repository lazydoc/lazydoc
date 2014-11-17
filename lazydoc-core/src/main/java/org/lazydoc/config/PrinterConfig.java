package org.lazydoc.config;

import org.lazydoc.model.DocDataType;
import org.lazydoc.model.DocDomain;
import org.lazydoc.model.DocError;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class PrinterConfig {
	
	private String className;
	private String outputPath;
    private Map<String, String> params = new HashMap<>();

    protected Map<Integer, DocDomain> domains;
    protected Map<String, DocDataType> dataTypes;
    protected Set<DocError> listOfCommonErrors = new TreeSet<DocError>();

    public PrinterConfig() {
        super();
    }

    public PrinterConfig(String className, String outputPath) {
        this.className = className;
        this.outputPath = outputPath;
    }

    public PrinterConfig(String className, String outputPath, Map<String, String> params) {
        this.className = className;
        this.outputPath = outputPath;
        this.params = params;
    }

    public String getClassName() {
		return className;
	}
	public void setClassName(String className) {
		this.className = className;
	}
	public String getOutputPath() {
		return outputPath;
	}
	public void setOutputPath(String outputPath) {
		this.outputPath = outputPath;
	}

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    public Map<Integer, DocDomain> getDomains() {
        return domains;
    }

    public void setDomains(Map<Integer, DocDomain> domains) {
        this.domains = domains;
    }

    public Map<String, DocDataType> getDataTypes() {
        return dataTypes;
    }

    public void setDataTypes(Map<String, DocDataType> dataTypes) {
        this.dataTypes = dataTypes;
    }

    public Set<DocError> getListOfCommonErrors() {
        return listOfCommonErrors;
    }

    public void setListOfCommonErrors(Set<DocError> listOfCommonErrors) {
        this.listOfCommonErrors = listOfCommonErrors;
    }

}
