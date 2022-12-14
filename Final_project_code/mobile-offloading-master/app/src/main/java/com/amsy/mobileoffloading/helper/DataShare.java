package com.amsy.mobileoffloading.helper;

import android.content.Context;

import com.amsy.mobileoffloading.entities.C_Workload;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.Payload;

import java.io.IOException;

public class DataShare {
    public static void transferWorkload(Context context, String endpointId, C_Workload tPayload) {
        try {
            Payload payload = WorkloadConvert.toPayload(tPayload);
            Nearby.getConnectionsClient(context).sendPayload(endpointId, payload);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

