package org.lazydoc.model;

public class DocEnum implements Comparable<DocEnum> {

    private int order = Integer.MAX_VALUE;
    private String value;
    private String description;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public int compareTo(DocEnum docEnum) {
        int result = Integer.compare(this.order, docEnum.order);
        if(result == 0) {
            return this.value.compareTo(docEnum.value);
        }
        return result;
    }
}
