package org.lazydoc.model;

import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.removeEnd;

public class DocDataType {
	private String name = "";
	private String id = "";
	private boolean list = false;
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

	public List<DocProperty> getProperties() {
		return properties;
	}

	public void setProperties(List<DocProperty> properties) {
		this.properties = properties;
	}
}