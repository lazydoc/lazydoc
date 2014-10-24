package org.lazydoc.model;

import static org.apache.commons.lang3.StringUtils.removeEnd;

public class DocParameter {
	private String paramType = "";
	private String name = "";
	private String referenceName = "";
	private String description = "";
	private String dataType = "";
	private Class<?> dataTypeClass;
	private boolean list = false;
	private boolean required = false;
	private boolean allowMultiple = false;
	private boolean ignore = false;

	public String getParamType() {
		return paramType;
	}

	public void setParamType(String paramType) {
		this.paramType = paramType;
	}

	public String getReferenceName() {
		return referenceName;
	}

	public void setReferenceName(String name) {
		this.referenceName = removeEnd(name, "VO");
	}

	public String getName() {
		return name;
	}

	public void setName(String shortName) {
		this.name = shortName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDataType() {
		return dataType;
	}

	public void setDataType(String dataType) {
		this.dataType = removeEnd(dataType, "VO");
	}

	public boolean isRequired() {
		return required;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

	public boolean isAllowMultiple() {
		return allowMultiple;
	}

	public void setAllowMultiple(boolean allowMultiple) {
		this.allowMultiple = allowMultiple;
	}

	public Class<?> getDataTypeClass() {
		return dataTypeClass;
	}

	public void setDataTypeClass(Class<?> dataTypeClass) {
		this.dataTypeClass = dataTypeClass;
		setDataType(dataTypeClass.getSimpleName());
	}

	public boolean isList() {
		return list;
	}

	public void setList(boolean list) {
		this.list = list;
	}

	public boolean isIgnore() {
		return ignore;
	}

	public void setIgnore(boolean ignore) {
		this.ignore = ignore;
	}

}