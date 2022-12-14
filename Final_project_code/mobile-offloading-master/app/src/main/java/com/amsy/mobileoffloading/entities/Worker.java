package com.amsy.mobileoffloading.entities;

import java.io.Serializable;

public class Worker implements Serializable {

    private String endpointId, endpointName;
    private WorkerInfo workerInfo;
    private WorkStatus workStatus;

    private int workQuantity;
    private float distanceFromMaster;

    public String getEndpointId() {
        return endpointId;
    }

    public void setEndpointId(String endpointId) {
        this.endpointId = endpointId;
    }

    public String getEndpointName() {
        return endpointName;
    }

    public void setEndpointName(String endpointName) {
        this.endpointName = endpointName;
    }

    public WorkStatus getWorkStatus() {
        return workStatus;
    }

    public void setWorkStatus(WorkStatus workStatus) {
        this.workStatus = workStatus;
    }

    public WorkerInfo getDeviceStats() {
        return workerInfo;
    }

    public void setDeviceStats(WorkerInfo deviceStats) {
        this.workerInfo = deviceStats;
    }

    public int getWorkAmount() {
        return workQuantity;
    }

    public void setWorkAmount(int workAmount) {
        this.workQuantity = workAmount;
    }

    public float getDistanceFromMaster() {
        return distanceFromMaster;
    }

    public void setDistanceFromMaster(float distanceFromMaster) {
        this.distanceFromMaster = distanceFromMaster;
    }

}
