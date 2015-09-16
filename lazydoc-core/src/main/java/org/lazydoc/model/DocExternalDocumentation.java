package org.lazydoc.model;

import org.lazydoc.annotation.InsertPosition;

public class DocExternalDocumentation {

    private String location;
    private InsertPosition postion = InsertPosition.TOP;

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public InsertPosition getPostion() {
        return postion;
    }

    public void setPostion(InsertPosition postion) {
        this.postion = postion;
    }
}
