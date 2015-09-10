package org.lazydoc.model;

/**
 * Created by eckart on 10.09.15.
 */
public class OperationResponse {

    private String responseType = "";
    private boolean inList = false;

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    public boolean isInList() {
        return inList;
    }

    public void setInList(boolean inList) {
        this.inList = inList;
    }

    @Override
    public String toString() {
        return "OperationResponse{" +
                "responseType='" + responseType + '\'' +
                ", inList=" + inList +
                '}';
    }
}
