package org.lazydoc.model;

public class DocError implements Comparable<DocError> {

	private int httpStatus;
	private String errorCode;
	private String description;

	public int getHttpStatus() {
		return httpStatus;
	}

	public void setHttpStatus(int httpStatus) {
		this.httpStatus = httpStatus;
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
		if (httpStatus == error.httpStatus) {
			int compareResult = errorCode.compareTo(error.errorCode);
			if (compareResult == 0) {
				return description.compareTo(error.description);
			}
			return compareResult;
		} else {
			return new Integer(httpStatus).compareTo(error.httpStatus);
		}
	}

}
