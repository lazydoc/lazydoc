package org.lazydoc.model;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.lazydoc.annotation.InsertPosition;

public class DocDomain {
	protected String domain = "";
	protected String domainShortDescription = "";
	private String description = "";
	private int order = 1;
	private String externalDocumentation;
	private InsertPosition externalInsertPosition = InsertPosition.TOP;
	private Set<DocOperation> operations = new TreeSet<DocOperation>();
	private Map<Integer, DocSubDomain> subDomains = new TreeMap<Integer, DocSubDomain>();
	private Set<DocError> errorList = new TreeSet<>();

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

	public String getExternalDocumentation() {
		return externalDocumentation;
	}

	public void setExternalDocumentation(String externalDocumentation) {
		this.externalDocumentation = externalDocumentation;
	}

	public InsertPosition getExternalInsertPosition() {
		return externalInsertPosition;
	}

	public void setExternalInsertPosition(InsertPosition externalInsertPosition) {
		this.externalInsertPosition = externalInsertPosition;
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
		return StringUtils.isNotBlank(externalDocumentation);
	}

}