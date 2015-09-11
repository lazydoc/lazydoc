package org.lazydoc.model;

import java.util.Set;
import java.util.TreeSet;

import static org.apache.commons.lang3.StringUtils.removeEnd;

public class DocDataType {
	private String name = "";
	private String id = "";
	private boolean list = false;
	private Set<DocProperty> properties = new TreeSet<>();

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

	public Set<DocProperty> getProperties() {
		return properties;
	}

	public void setProperties(Set<DocProperty> properties) {
		this.properties = properties;
	}
}