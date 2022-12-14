package com.amsy.mobileoffloading.services;

import android.content.Context;

import com.amsy.mobileoffloading.callback.WorkloadListener;
import com.amsy.mobileoffloading.callback.WorkerUpdateListener;
import com.amsy.mobileoffloading.entities.C_Workload;
import com.amsy.mobileoffloading.entities.WorkerInfo;
import com.amsy.mobileoffloading.entities.WorkStatus;
import com.amsy.mobileoffloading.helper.Constants;
import com.amsy.mobileoffloading.helper.WorkloadConvert;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;

import java.io.IOException;

public class WorkerStatusSubscriber {

    private Context context;
    private String endpointId;
    private WorkloadListener workloadListener;
    private WorkerUpdateListener workerUpdateListener;

    public WorkerStatusSubscriber(Context context, String endpointId, WorkerUpdateListener workerUpdateListener) {
        this.context = context;
        this.endpointId = endpointId;
        this.workerUpdateListener = workerUpdateListener;
    }

    public void start() {
        workloadListener = new WorkloadListener() {
            @Override
            public void onPayloadReceived(String endpointId, Payload payload) {
                try {
                    C_Workload tPayload = (C_Workload) WorkloadConvert.fromPayload(payload);
                    String payloadTag = tPayload.getTag();

                    if (payloadTag.equals(Constants.PayloadTags.WORK_STATUS)) {
                        if (workerUpdateListener != null) {
                            workerUpdateListener.onWorkStatusReceived(endpointId, (WorkStatus) tPayload.getData());
                        }
                    } else if (payloadTag.equals(Constants.PayloadTags.DEVICE_STATS)) {
                        if (workerUpdateListener != null) {
                            workerUpdateListener.onDeviceStatsReceived(endpointId, (WorkerInfo) tPayload.getData());
                        }
                    }
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate payloadTransferUpdate) {

            }
        };

        NearbyConnectionsManager.getInstance(context).registerPayloadListener(workloadListener);
        NearbyConnectionsManager.getInstance(context).acceptConnection(endpointId);
    }

    public void stop() {
        NearbyConnectionsManager.getInstance(context).unregisterPayloadListener(workloadListener);
    }

}
