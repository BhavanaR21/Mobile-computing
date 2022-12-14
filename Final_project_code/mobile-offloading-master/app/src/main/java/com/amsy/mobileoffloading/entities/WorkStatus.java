package com.amsy.mobileoffloading.entities;

import java.io.Serializable;

public class WorkStatus implements Serializable {
    private int result;
    private int partIdx;
    private String status;

    public int getResultInfo() {
        return result;
    }

    public String getStatusInfo() {
        return status;
    }

    public void setStatusInfo(String status) {
        this.status = status;
    }

    public void setPartitionIndexInfo(int partitionIndex) {
        this.partIdx = partitionIndex;
    }

    public void setResultInfo(int result) {
        this.result = result;
    }

    public int getPartitionIndexInfo() {
        return partIdx;
    }




}
