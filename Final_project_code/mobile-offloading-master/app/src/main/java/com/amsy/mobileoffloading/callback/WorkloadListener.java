package com.amsy.mobileoffloading.callback;

import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;

public interface WorkloadListener {
    void onPayloadReceived(String endpointId, Payload payload);
    void onPayloadTransferUpdate( String endpointId,  PayloadTransferUpdate payloadTransferUpdate);
}

