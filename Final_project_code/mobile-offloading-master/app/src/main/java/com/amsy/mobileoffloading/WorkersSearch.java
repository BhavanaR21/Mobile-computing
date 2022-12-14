package com.amsy.mobileoffloading;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import com.amsy.mobileoffloading.adapters.adapterLinkedDevices;
import com.amsy.mobileoffloading.callback.ClientLinkedListener;
import com.amsy.mobileoffloading.callback.WorkloadListener;
import com.amsy.mobileoffloading.entities.C_Workload;
import com.amsy.mobileoffloading.entities.LinkedDevices;
import com.amsy.mobileoffloading.entities.WorkerInfo;
import com.amsy.mobileoffloading.helper.Constants;
//import com.amsy.mobileoffloading.helper.FlushToFile;
//import com.amsy.mobileoffloading.helper.MatrixDS;
import com.amsy.mobileoffloading.helper.WorkloadConvert;
import com.amsy.mobileoffloading.services.Connector;
import com.amsy.mobileoffloading.services.WorkersSearchService;
import com.amsy.mobileoffloading.services.NearbyConnectionsManager;
import com.amsy.mobileoffloading.services.WorkAllocator;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WorkersSearch extends AppCompatActivity {

    private RecyclerView linkedRVDevices;
    private adapterLinkedDevices adapterLinkedDevices;
    private List<LinkedDevices> linkedDevices = new ArrayList<>();
    private WorkersSearchService workersSearchService;
    private ClientLinkedListener clientLinkedListener;
    private WorkloadListener workloadListener;

    @Override
    protected void onPause() {
        super.onPause();
        setStatus("Stopped", false);
        workersSearchService.stop();
        NearbyConnectionsManager.getInstance(getApplicationContext()).unregisterPayloadListener(workloadListener);
        NearbyConnectionsManager.getInstance(getApplicationContext()).unregisterClientConnectionListener(clientLinkedListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startWorkerSearch();
        NearbyConnectionsManager.getInstance(getApplicationContext()).registerPayloadListener(workloadListener);
        NearbyConnectionsManager.getInstance(getApplicationContext()).registerClientConnectionListener(clientLinkedListener);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master_discovery);

        linkedRVDevices = findViewById(R.id.rv_connected_devices);
        adapterLinkedDevices = new adapterLinkedDevices(this, linkedDevices);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        linkedRVDevices.setLayoutManager(linearLayoutManager);

        linkedRVDevices.setAdapter(adapterLinkedDevices);
        adapterLinkedDevices.notifyDataSetChanged();

        workloadListener = new WorkloadListener() {
            @Override
            public void onPayloadReceived(String endpointId, Payload payload) {
                Log.d("MASTER_DISCOVERY", "PayloadListener -  onPayloadReceived");
                try {
                    C_Workload tPayload = WorkloadConvert.fromPayload(payload);
                    if (tPayload.getTag().equals(Constants.PayloadTags.DEVICE_STATS)) {
                        updateDeviceStats(endpointId, (WorkerInfo) tPayload.getData());
                    }
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }


            @Override
            public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate payloadTransferUpdate) {
                Log.d("MASTER_DISCOVERY", "PayloadListener -  onPayloadTransferUpdate");
            }
        };


        clientLinkedListener = new ClientLinkedListener() {
            @Override
            public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                Log.d("MASTER_DISCOVERY", "clientConnectionListener -  onConnectionInitiated");
                NearbyConnectionsManager.getInstance(getApplicationContext()).acceptConnection(endpointId);
            }

            @Override
            public void onConnectionResult(String endpointId, ConnectionResolution connectionResolution) {

                Log.d("MASTER_DISCOVERY", "clientConnectionListener -  onConnectionResult" + endpointId);

                int statusCode = connectionResolution.getStatus().getStatusCode();
                if (statusCode == ConnectionsStatusCodes.STATUS_OK) {
                    Log.d("MASTER_DISCOVERY", "clientConnectionListener -  onConnectionResult - ACCEPTED");
                    updateConnectedDeviceRequestStatus(endpointId, Constants.RequestStatus.ACCEPTED);
                } else if (statusCode == ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED) {
                    Log.d("MASTER_DISCOVERY", "clientConnectionListener -  onConnectionResult - REJECTED");
                    updateConnectedDeviceRequestStatus(endpointId, Constants.RequestStatus.REJECTED);
                } else if (statusCode == ConnectionsStatusCodes.STATUS_ERROR) {
                    Log.d("MASTER_DISCOVERY", "clientConnectionListener -  onConnectionResult - ERROR");
                    removeConnectedDevice(endpointId, true);
                }
            }

            @Override
            public void onDisconnected(String endpointId) {
                Log.d("MASTER_DISCOVERY", "clientConnectionListener -  onDisconnected " + endpointId);
                removeConnectedDevice(endpointId, true);
            }
        };


    }


    public void assignTasks(View view) {
        ArrayList<LinkedDevices> readyDevices = getDevicesInReadyState();
        if (readyDevices.size() == 0) {
            Toast.makeText(getApplicationContext(), "No worker Available at the moment", Toast.LENGTH_LONG).show();
            onBackPressed();
        } else {
            workersSearchService.stop();
            startMasterActivity(readyDevices);
            finish();
        }
    }

    private ArrayList<LinkedDevices> getDevicesInReadyState() {
        ArrayList<LinkedDevices> res = new ArrayList<>();
        for (int i = 0; i < linkedDevices.size(); i++) {
            if (linkedDevices.get(i).getPermissionStatus().equals(Constants.RequestStatus.ACCEPTED)) {
                if (linkedDevices.get(i).getDeviceInfo().getChargeLevel() > WorkAllocator.ThresholdsHolder.MINIMUM_BATTERY_LEVEL) {
                    res.add(linkedDevices.get(i));
                } else {
                    C_Workload tPayload = new C_Workload();
                    tPayload.setTag(Constants.PayloadTags.DISCONNECTED);

                    Connector.sendToDevice(getApplicationContext(), linkedDevices.get(i).getConnectID(), tPayload);
                }
            } else {
                Log.d("MASTER_DISCOVERY", "LOOPING");
                C_Workload tPayload = new C_Workload();
                tPayload.setTag(Constants.PayloadTags.DISCONNECTED);

                Connector.sendToDevice(getApplicationContext(), linkedDevices.get(i).getConnectID(), tPayload);
            }

        }
        return res;
    }

    private void updateConnectedDeviceRequestStatus(String endpointId, String status) {
        for (int i = 0; i < linkedDevices.size(); i++) {
            if (linkedDevices.get(i).getConnectID().equals(endpointId)) {
                linkedDevices.get(i).setPermissionStatus(status);
                Log.d("MASTER_DISCOVERY", "Status of end point set to "+status);
                adapterLinkedDevices.notifyItemChanged(i);
                break;
            }
        }
    }

    private void startWorkerSearch() {
                Log.d("MASTER_DISCOVERY", "Starting Master Discovery");
        EndpointDiscoveryCallback endpointDiscoveryCallback = new EndpointDiscoveryCallback() {
            @Override
            public void onEndpointFound(@NonNull String endpointId, @NonNull DiscoveredEndpointInfo discoveredEndpointInfo) {
                Log.d("MASTER_DISCOVERY", "ENDPOINT FOUND " +endpointId);
                Log.d("MASTER_DISCOVERY", endpointId);
                Log.d("MASTER_DISCOVERY", discoveredEndpointInfo.getServiceId() + " " + discoveredEndpointInfo.getEndpointName());

                LinkedDevices LinkedDevices = new LinkedDevices();
                LinkedDevices.setConnectID(endpointId);
                LinkedDevices.setConnectName(discoveredEndpointInfo.getEndpointName());
                LinkedDevices.setPermissionStatus(Constants.RequestStatus.PENDING);
                LinkedDevices.setDeviceInfo(new WorkerInfo());

                linkedDevices.add(LinkedDevices);
                adapterLinkedDevices.notifyItemChanged(linkedDevices.size() - 1);

                Log.d("MASTER_DISCOVERY", "Added end point to connected devices : " +endpointId);

                NearbyConnectionsManager.getInstance(getApplicationContext()).requestConnection(endpointId, "MASTER");
                Log.d("MASTER_DISCOVERY", "Requested connection for : " +endpointId);

            }

            @Override
            public void onEndpointLost(@NonNull String endpointId) {
                Log.d("MASTER_DISCOVERY", "ENDPOINT LOST");
                Log.d("MASTER_DISCOVERY", endpointId);
                removeConnectedDevice(endpointId, false);
            }
        };

        workersSearchService = new WorkersSearchService(this);
        workersSearchService.start(endpointDiscoveryCallback)
                .addOnSuccessListener((unused) -> {
                    setStatus("Searching...", true);
                })
                .addOnFailureListener(command -> {
                    if (((ApiException) command).getStatusCode() == 8002) {
                        setStatus("Still Searching...", true);
                    } else {
                        setStatus("Discovering Failed", false);
                        finish();
                    }
                    command.printStackTrace();
                });
        ;
    }

    private void removeConnectedDevice(String endpointId, boolean forceRemove) {

        for (int i = 0; i < linkedDevices.size(); i++) {
            boolean checkStatus = forceRemove ? true :  !linkedDevices.get(i).getPermissionStatus().equals(Constants.RequestStatus.ACCEPTED);
            if (linkedDevices.get(i).getConnectID().equals(endpointId) && checkStatus) {
                Log.d("MASTER_DISCOVERY", "Removed end point from connected devices " + endpointId );
                linkedDevices.remove(i);
                adapterLinkedDevices.notifyItemChanged(i);
                break;
            }
        }
    }

    private void updateDeviceStats(String endpointId, WorkerInfo deviceStats) {
        canAssign(deviceStats);
        for (int i = 0; i < linkedDevices.size(); i++) {
            if (linkedDevices.get(i).getConnectID().equals(endpointId)) {
                linkedDevices.get(i).setDeviceInfo(deviceStats);

//                Toast.makeText(getApplicationContext(), "Success: updated battery level: can proceed", Toast.LENGTH_SHORT).show();
                linkedDevices.get(i).setPermissionStatus(Constants.RequestStatus.ACCEPTED);
                adapterLinkedDevices.notifyItemChanged(i);
                break;
            }
        }
    }

    void canAssign(WorkerInfo deviceStats) {
        Button assignButton = findViewById(R.id.assignTask);
        assignButton.setVisibility(deviceStats.getChargeLevel() > WorkAllocator.ThresholdsHolder.MINIMUM_BATTERY_LEVEL ? View.VISIBLE : View.INVISIBLE);
    }

    void setStatus(String text, boolean search) {
        TextView disc = findViewById(R.id.discovery);
        disc.setText(text);
        ProgressBar pb = findViewById(R.id.progressBar);
        pb.setIndeterminate(search);
    }

    @Override
    public void finish() {
        super.finish();
        workersSearchService.stop();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    private void startMasterActivity(ArrayList<LinkedDevices> linkedDevices) {
        Intent intent = new Intent(getApplicationContext(), MasterActivity.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable(Constants.CONNECTED_DEVICES, linkedDevices);
        intent.putExtras(bundle);
        startActivity(intent);
        finish();
    }

}