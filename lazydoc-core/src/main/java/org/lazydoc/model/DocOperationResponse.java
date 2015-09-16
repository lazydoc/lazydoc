package org.lazydoc.model;

/**
 * Created by eckart on 10.09.15.
 */
public class DocOperationResponse {

    private String responseType = "";
    private boolean inList = false;
    private String description  = "";
    private String staticSample = "";

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStaticSample() {
        return staticSample;
    }

    public void setStaticSample(String staticSample) {
        this.staticSample = staticSample;
    }

    @Override
    public String toString() {
        return "OperationResponse{" +
                "responseType='" + responseType + '\'' +
                ", inList=" + inList +
                '}';
    }
}
