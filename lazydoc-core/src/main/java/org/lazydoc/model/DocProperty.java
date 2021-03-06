package org.lazydoc.model;

import org.apache.commons.lang3.StringUtils;

import java.util.*;

import static org.apache.commons.lang3.StringUtils.removeEnd;

public class DocProperty implements Comparable<DocProperty> {
	private int order = Integer.MAX_VALUE;
	private String name = "";
	private String description = "";
	private String mapKeyDescription = "";
	private String mapValueDescription = "";
	private String type = "";
	private String[] sample = {};
	private Set<DocEnum> enumValues = new TreeSet<>();
    private boolean addEnumValues = true;
	private boolean required = false;
	private boolean request = true;
	private boolean response = true;
	private boolean list = false;
	private boolean map = false;
	private boolean deprecated = false;
	private boolean primitive = false;
	private boolean requestNullValueSample = true;
	private boolean responseNullValueSample = true;

	public boolean hasEnumValues() {
		return !enumValues.isEmpty();
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

	public String getMapKeyDescription() {
		return mapKeyDescription;
	}

	public void setMapKeyDescription(String mapKeyDescription) {
		this.mapKeyDescription = mapKeyDescription;
	}

	public String getMapValueDescription() {
		return mapValueDescription;
	}

	public void setMapValueDescription(String mapValueDescription) {
		this.mapValueDescription = mapValueDescription;
	}

	public boolean isMap() {
		return map;
	}

	public void setMap(boolean map) {
		this.map = map;
	}

	public boolean isPrimitive() {
		return primitive;
	}

	public void setPrimitive(boolean primitive) {
		this.primitive = primitive;
	}

	public boolean isRequestNullValueSample() {
		return requestNullValueSample;
	}

	public void setRequestNullValueSample(boolean requestNullValueSample) {
		this.requestNullValueSample = requestNullValueSample;
	}

	public boolean isResponseNullValueSample() {
		return responseNullValueSample;
	}

	public void setResponseNullValueSample(boolean responseNullValueSample) {
		this.responseNullValueSample = responseNullValueSample;
	}

    public Set<DocEnum> getEnumValues() {
        return enumValues;
    }

    public void setEnumValues(Set<DocEnum> enumValues) {
        this.enumValues = enumValues;
    }

    public boolean isAddEnumValues() {
        return addEnumValues;
    }

    public void setAddEnumValues(boolean addEnumValues) {
        this.addEnumValues = addEnumValues;
    }

    @Override
	public int compareTo(DocProperty property) {
		int compareResult = Integer.compare(this.order, property.order);
		if(compareResult == 0) {
			if(this.required == property.required) {
				return this.name.compareTo(property.name);
			} else {
				return this.required ? -1 : 1;
			}
		}
		return compareResult;
	}

	@Override
	public String toString() {
		return "DocProperty{" +
				"order=" + order +
				", name='" + name + '\'' +
				", description='" + description + '\'' +
				", mapKeyDescription='" + mapKeyDescription + '\'' +
				", mapValueDescription='" + mapValueDescription + '\'' +
				", type='" + type + '\'' +
				", sample=" + Arrays.toString(sample) +
				", enumValues=" + enumValues +
				", required=" + required +
				", request=" + request +
				", response=" + response +
				", list=" + list +
				", map=" + map +
				", deprecated=" + deprecated +
				", primitive=" + primitive +
				", requestNullValueSample=" + requestNullValueSample +
				", responseNullValueSample=" + responseNullValueSample +
				'}';
	}
}
