package com.amsy.mobileoffloading.entities;

import java.io.Serializable;

public class LinkedDevices implements Serializable {
    private String connectID;
    private String connectName;
    private WorkerInfo deviceInfo;
    private String permissionStatus;


    public String getConnectID() {
        return connectID;
    }

    public void setConnectID(String connectID) {
        this.connectID = connectID;
    }

    public String getConnectName() {
        return connectName;
    }

    public void setConnectName(String connectName) {
        this.connectName = connectName;
    }

    public WorkerInfo getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(WorkerInfo deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public String getPermissionStatus() {
        return permissionStatus;
    }

    public void setPermissionStatus(String permissionStatus) {
        this.permissionStatus = permissionStatus;
    }
}
