package org.lazydoc.model;

import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class DocDomain {
    protected String domain = "";
    protected String domainShortDescription = "";
    private String description = "";
    private int order = 1;
    private List<DocExternalDocumentation> externalDocumentations = new ArrayList<>();
    private Set<DocOperation> operations = new TreeSet<>();
    private Map<Integer, DocSubDomain> subDomains = new TreeMap<>();
    private Set<DocError> errorList = new TreeSet<>();
    private boolean deprecated = false;

    public String getDomain() {
        return domain;
    }

    public void setDomain(String name) {
        this.domain = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDomainShortDescription(String domainShortDescription) {
        this.domainShortDescription = domainShortDescription;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public Map<Integer, DocSubDomain> getSubDomains() {
        return subDomains;
    }

    public void setSubDomains(Map<Integer, DocSubDomain> subDomains) {
        this.subDomains = subDomains;
    }

    public Set<DocOperation> getOperations() {
        return operations;
    }

    public void setOperations(Set<DocOperation> operations) {
        this.operations = operations;
    }

    public List<DocExternalDocumentation> getExternalDocumentations() {
        return externalDocumentations;
    }

    public void setExternalDocumentations(List<DocExternalDocumentation> externalDocumentations) {
        this.externalDocumentations = externalDocumentations;
    }

    public Set<DocError> getErrorList() {
        return errorList;
    }

    public void setErrorList(Set<DocError> errorList) {
        this.errorList = errorList;
    }

    public void addSubDomain(DocSubDomain subDomain) {
        this.getSubDomains().put(subDomain.getOrder(), subDomain);
    }

    public void addDocError(DocError error) {
        errorList.add(error);
    }

    public String getDomainShortDescription() {
        if (StringUtils.isNotBlank(domainShortDescription)) {
            return domainShortDescription;
        }
        return domain;
    }

    public boolean hasExternalDocumentation() {
        return !externalDocumentations.isEmpty();
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }
}