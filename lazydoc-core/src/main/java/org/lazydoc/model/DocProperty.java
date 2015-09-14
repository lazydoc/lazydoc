package org.lazydoc.model;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.removeEnd;

public class DocProperty implements Comparable<DocProperty> {
	private int order = Integer.MAX_VALUE;
	private String name = "";
	private String description = "";
	private String type = "";
	private String[] sample = {};
	private List<String> enumValues = new ArrayList<String>();
	private boolean required = false;
	private boolean request = true;
	private boolean response = true;
	private boolean list = false;
	private boolean deprecated = false;

	public boolean hasEnumValues() {
		return !enumValues.isEmpty();
	}

	public void addEnumValues(Class<?> propertyType) {
		if (propertyType.isEnum()) {
			for (Enum<?> enumElement : (Enum[]) propertyType.getEnumConstants()) {
				enumValues.add(enumElement.toString());
			}
		}
	}

    public void  addEnumValue(String value) {
        enumValues.add(value);
    }

	public String getEnumValues() {
		return StringUtils.join(enumValues, ", ");
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = removeEnd(type, "VO");
	}

	public String getSampleString() {
		if (sample.length == 1) {
			return "\"" + sample[0] + "\"";
		} else {
			return "[\"" + StringUtils.join(sample, "\",\"") + "\"]";
		}

	}

	public String[] getSample() {
		return sample;
	}

	public void setSample(String[] sample) {
		this.sample = sample;
	}

	public boolean isRequired() {
		return required;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

	public boolean isRequest() {
		return request;
	}

	public void setRequest(boolean request) {
		this.request = request;
	}

	public boolean isResponse() {
		return response;
	}

	public void setResponse(boolean response) {
		this.response = response;
	}

	public boolean isList() {
		return list;
	}

	public void setList(boolean list) {
		this.list = list;
	}

	public boolean hasSample() {
		return sample.length > 0;
	}

	public boolean isDeprecated() {
		return deprecated;
	}

	public void setDeprecated(boolean deprecated) {
		this.deprecated = deprecated;
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int compareTo(DocProperty property) {
		int compareResult = Integer.compare(this.order, property.order);
		if(compareResult == 0) {
			return this.name.compareTo(property.name);
		}
		return compareResult;
	}
}
