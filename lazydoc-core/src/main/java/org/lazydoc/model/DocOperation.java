package org.lazydoc.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.lazydoc.annotation.InsertPosition;

public class DocOperation implements Comparable<DocOperation> {
	private Integer order = 0;
	private String fileName;
	private String path = "";
	private String pathDescription = "";
	private String httpMethod = "";
	private String nickname = "";
	private String responseClass = "";
	private String shortDescription = "";
	private String summary = "";
	private String notes = "";
	private String errorResponse = "";
	private String level = "plattform";
	private List<DocParameter> parameters = new ArrayList<DocParameter>();
	private String role;
	private String externalDocumentation;
	private InsertPosition externalInsertPosition = InsertPosition.TOP;

	public Integer getOrder() {
		return order;
	}

	public void setOrder(Integer order) {
		this.order = order;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getPathDescription() {
		return pathDescription;
	}

	public void setPathDescription(String pathDescription) {
		this.pathDescription = pathDescription;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getHttpMethod() {
		return httpMethod;
	}

	public void setHttpMethod(String httpMethod) {
		this.httpMethod = httpMethod;
	}

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public String getResponseClass() {
		return responseClass;
	}

	public void setResponseClass(String responseClass) {
		this.responseClass = responseClass;
	}

	public String getShortDescription() {
		if (StringUtils.isBlank(shortDescription)) {
			return summary;
		}
		return shortDescription;
	}

	public void setShortDescription(String shortDescription) {
		this.shortDescription = shortDescription;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public String getErrorResponse() {
		return errorResponse;
	}

	public void setErrorResponse(String errorResponse) {
		this.errorResponse = errorResponse;
	}

	public String getLevel() {
		return level;
	}

	public void setLevel(String level) {
		this.level = level;
	}

	public List<DocParameter> getParameters() {
		return parameters;
	}

	public void setParameters(List<DocParameter> parameters) {
		this.parameters = parameters;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	@Override
	public int compareTo(DocOperation o) {
		int compareResult = this.order.compareTo(o.order);
		if (compareResult == 0) {
			return this.nickname.compareTo(o.nickname);
		}
		return compareResult;
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

	public boolean hasExternalDocumentation() {
		return StringUtils.isNotBlank(externalDocumentation);
	}
}