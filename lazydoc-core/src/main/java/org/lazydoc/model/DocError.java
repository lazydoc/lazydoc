package org.lazydoc.model;

public class DocError implements Comparable<DocError> {

	private int statusCode;
	private String errorCode;
	private String description;

	public DocError() {
	}

	public DocError(int statusCode, String errorCode, String description) {
		this.statusCode = statusCode;
		this.errorCode = errorCode;
		this.description = description;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public int compareTo(DocError error) {
		if (statusCode == error.statusCode) {
			int compareResult = errorCode.compareTo(error.errorCode);
			if (compareResult == 0) {
				return description.compareTo(error.description);
			}
			return compareResult;
		} else {
			return new Integer(statusCode).compareTo(error.statusCode);
		}
	}

	@Override
	public String toString() {
		return "DocError{" +
				"statusCode=" + statusCode +
				", errorCode='" + errorCode + '\'' +
				", description='" + description + '\'' +
				'}';
	}
}
