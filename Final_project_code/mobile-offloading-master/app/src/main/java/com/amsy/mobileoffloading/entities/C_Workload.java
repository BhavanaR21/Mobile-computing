package com.amsy.mobileoffloading.entities;

import java.io.Serializable;

public class C_Workload implements Serializable {
    private String tag;
    private Object data;

    public String getTag() {
        return tag;
    }

    public C_Workload setTag(String tag) {
        this.tag = tag;
        return this;
    }

    public Object getData() {
        return data;
    }

    public C_Workload setData(Object data) {
        this.data = data;
        return this;
    }
}
