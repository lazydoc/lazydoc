package org.lazydoc.model;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.removeEnd;

public class DocDataType {
	private String name = "";
	private String id = "";
	private boolean list = false;
	private boolean nullValuesInSample = true;
	private String alias = "";

	private List<DocProperty> properties = new ArrayList<>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = removeEnd(name, "VO");
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public boolean isList() {
		return list;
	}

	public void setList(boolean list) {
		this.list = list;
	}

	public boolean isNullValuesInSample() {
		return nullValuesInSample;
	}

	public void setNullValuesInSample(boolean nullValuesInSample) {
		this.nullValuesInSample = nullValuesInSample;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public List<DocProperty> getProperties() {
		return properties;
	}

	public void setProperties(List<DocProperty> properties) {
		this.properties = properties;
	}

	public String getAliasOrName() {
		return StringUtils.isNotBlank(alias) ? alias : name;
	}

	@Override
	public String toString() {
		return "DocDataType{" +
				"name='" + name + '\'' +
				", id='" + id + '\'' +
				", list=" + list +
				", nullValuesInSample=" + nullValuesInSample +
				", alias='" + alias + '\'' +
				", properties=" + properties +
				'}';
	}
}