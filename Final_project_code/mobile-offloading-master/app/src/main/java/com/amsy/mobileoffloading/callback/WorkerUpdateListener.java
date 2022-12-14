package com.amsy.mobileoffloading.callback;

import com.amsy.mobileoffloading.entities.WorkerInfo;
import com.amsy.mobileoffloading.entities.WorkStatus;

public interface WorkerUpdateListener {

    void onWorkStatusReceived(String endpointId, WorkStatus workStatus);

     void onDeviceStatsReceived(String endpointId, WorkerInfo deviceStats);

}
