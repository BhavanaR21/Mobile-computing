package com.amsy.mobileoffloading.services;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.amsy.mobileoffloading.callback.ClientLinkedListener;
import com.amsy.mobileoffloading.callback.WorkloadListener;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.tasks.Task;

import java.util.HashSet;

public class NearbyConnectionsManager {

    private static NearbyConnectionsManager nearbyConnectionsManager;
    private Context context;

    private ConnectionLifecycleCallback connectionLifecycleCallback;
    private HashSet<ClientLinkedListener> clientConnectionListenerSet = new HashSet<>();

    private HashSet<WorkloadListener> workloadListenersSet = new HashSet<>();

    public NearbyConnectionsManager(Context context) {
        this.context = context;
        this.connectionLifecycleCallback = new ConnectionLifecycleCallback() {
            @Override
            public void onConnectionInitiated(@NonNull String endpointId, @NonNull ConnectionInfo connectionInfo) {
                for (ClientLinkedListener clientConnectionListener : clientConnectionListenerSet) {
                    try {
                        clientConnectionListener.onConnectionInitiated(endpointId, connectionInfo);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onConnectionResult(@NonNull String endpointId, @NonNull ConnectionResolution connectionResolution) {
                for (ClientLinkedListener clientConnectionListener : clientConnectionListenerSet) {
                    try {
                        clientConnectionListener.onConnectionResult(endpointId, connectionResolution);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onDisconnected(@NonNull String endpointId) {
                Toast.makeText(context, "Disconnected", Toast.LENGTH_SHORT).show();
                for (ClientLinkedListener clientConnectionListener : clientConnectionListenerSet) {
                    try {
                        clientConnectionListener.onDisconnected(endpointId);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };
    }

    public static NearbyConnectionsManager getInstance(Context context) {
        if (nearbyConnectionsManager == null) {
            nearbyConnectionsManager = new NearbyConnectionsManager(context);
        }

        return nearbyConnectionsManager;
    }

    public void requestConnection(String endpointId, String clientId) {
        Nearby.getConnectionsClient(context)
                .requestConnection(clientId, endpointId, connectionLifecycleCallback)
                .addOnSuccessListener(unused -> {
                    Log.d("NEARBYCONNCTNMGR", "CONNECTION REQUESTED");
                })
                .addOnFailureListener((Exception e) -> {
                    Log.d("NEARBYCONNCTNMGR", "CONNECTION FAILED");
                    e.printStackTrace();
                });
    }

    public void acceptConnection(String endpointId) {
        Nearby.getConnectionsClient(context).acceptConnection(endpointId, new PayloadCallback() {
            @Override
            public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {
                for (WorkloadListener workloadListener : workloadListenersSet) {
                    try {
                        workloadListener.onPayloadReceived(endpointId, payload);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onPayloadTransferUpdate(@NonNull String endpointId, @NonNull PayloadTransferUpdate payloadTransferUpdate) {

            }
        });
    }

    public void rejectConnection(String endpointId) {
        Nearby.getConnectionsClient(context).rejectConnection(endpointId);
    }

    public void disconnectFromEndpoint(String endpointId) {
        Nearby.getConnectionsClient(context).disconnectFromEndpoint(endpointId);
    }

    public Task<Void> advertise(String clientId, AdvertisingOptions advertisingOptions) {
        return Nearby.getConnectionsClient(context)
                .startAdvertising(clientId, context.getPackageName(), connectionLifecycleCallback, advertisingOptions)
                .addOnFailureListener((Exception e) -> {
                    e.printStackTrace();
                });
    }

    public boolean registerPayloadListener(WorkloadListener workloadListener) {
        if (workloadListener != null) {
            return workloadListenersSet.add(workloadListener);
        }
        return false;
    }

    public boolean registerClientConnectionListener(ClientLinkedListener clientConnectionListener) {
        if (clientConnectionListener != null) {
            return clientConnectionListenerSet.add(clientConnectionListener);
        }
        return false;
    }

    public boolean unregisterPayloadListener(WorkloadListener workloadListener) {
        if (workloadListener != null) {
            return workloadListenersSet.remove(workloadListener);
        }
        return false;
    }


    public boolean unregisterClientConnectionListener(ClientLinkedListener clientConnectionListener) {
        if (clientConnectionListener != null) {
            return clientConnectionListenerSet.remove(clientConnectionListener);
        }
        return false;
    }


}
