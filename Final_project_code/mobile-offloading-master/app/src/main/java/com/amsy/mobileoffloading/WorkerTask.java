package com.amsy.mobileoffloading;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.BatteryManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.amsy.mobileoffloading.callback.ClientLinkedListener;
import com.amsy.mobileoffloading.callback.WorkloadListener;
import com.amsy.mobileoffloading.entities.C_Workload;
import com.amsy.mobileoffloading.entities.TaskInfo;
import com.amsy.mobileoffloading.entities.WorkStatus;
import com.amsy.mobileoffloading.helper.Constants;
import com.amsy.mobileoffloading.helper.DataShare;
import com.amsy.mobileoffloading.helper.Matrixstruct;
import com.amsy.mobileoffloading.helper.WorkloadConvert;
import com.amsy.mobileoffloading.services.DeviceStatisticsPublisher;
import com.amsy.mobileoffloading.services.NearbyConnectionsManager;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;

import java.io.IOException;
import java.util.HashSet;

import pl.droidsonroids.gif.GifImageView;

public class WorkerTask extends AppCompatActivity {
    private String masterId;
    private DeviceStatisticsPublisher deviceStatsPublisher;
    private ClientLinkedListener connectionListener;
    private WorkloadListener payloadCallback;
    private int currPartIdx;
    private HashSet<Integer> completedTask = new HashSet<>();
    BatteryManager mBatteryManager = null;
    Long initialEnergyWorker,finalEnergyWorker,energyConsumedWorker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_computation);
        extractBundle();
        startDeviceStatsPublisher();
        setConnectionCallback();
        connectToMaster();
        //start measuring the pwer consumption at Worker
        mBatteryManager = (BatteryManager)getSystemService(Context.BATTERY_SERVICE);
        initialEnergyWorker =
                mBatteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER);
        Log.d("WORKER_COMPUTATION", "Capturing power consumption");
    }

    public void connectionInfo(String text, boolean isWorking) {
        //UI Textview
        TextView statusText = findViewById(R.id.statusText);
        statusText.setText(text);
        GifImageView waiting = findViewById(R.id.waiting);
        waiting.setVisibility(isWorking ? View.INVISIBLE : View.VISIBLE);
        GifImageView working = findViewById(R.id.working);
        working.setVisibility(isWorking ? View.VISIBLE : View.INVISIBLE);
    }

    public void onTaskCompleted(String text) {
        //UI Textview
        TextView statusText = findViewById(R.id.statusText);
        statusText.setText(text);
        GifImageView waiting = findViewById(R.id.waiting);
        waiting.setVisibility(View.INVISIBLE);
        GifImageView working = findViewById(R.id.working);
        working.setVisibility(View.INVISIBLE);
        GifImageView done = findViewById(R.id.done);
        done.setVisibility(View.VISIBLE);
        TextView powerConsumed = findViewById(R.id.powerValue);
        powerConsumed.setText("Power Consumption (Slave) : "  + Long.toString(energyConsumedWorker)+ " nWh");
    }


    public void setPartView(int count) {
//        TextView dispCount = findViewById(R.id.count);
//        //TODO : ANVESH
//        dispCount.setText(count + "");
    }

    private void extractBundle() {
        Bundle bundle = getIntent().getExtras();
        this.masterId = bundle.getString(Constants.MASTER_ENDPOINT_ID);
    }

    private void startDeviceStatsPublisher() {
        deviceStatsPublisher = new DeviceStatisticsPublisher(getApplicationContext(), masterId, Constants.UPDATE_INTERVAL_UI);
    }

    private void connectToMaster() {
        payloadCallback = new WorkloadListener() {
            @Override
            public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {
                beginTask(payload);
            }

            @Override
            public void onPayloadTransferUpdate(@NonNull String endpointId, @NonNull PayloadTransferUpdate payloadTransferUpdate) {

            }
        };
        NearbyConnectionsManager.getInstance(getApplicationContext()).acceptConnection(masterId);
    }

    private void setConnectionCallback() {
        connectionListener = new ClientLinkedListener() {
            @Override
            public void onConnectionInitiated(String id, ConnectionInfo connectionInfo) {
            }

            @Override
            public void onConnectionResult(String id, ConnectionResolution connectionResolution) {
            }

            @Override
            public void onDisconnected(String id) {
                goBack();
            }
        };
    }

    private void goBack() {
        completedTask = new HashSet<>();
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        NearbyConnectionsManager.getInstance(getApplicationContext()).registerPayloadListener(payloadCallback);
        NearbyConnectionsManager.getInstance(getApplicationContext()).registerClientConnectionListener(connectionListener);
        deviceStatsPublisher.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        NearbyConnectionsManager.getInstance(getApplicationContext()).unregisterPayloadListener(payloadCallback);
        NearbyConnectionsManager.getInstance(getApplicationContext()).unregisterClientConnectionListener(connectionListener);
        deviceStatsPublisher.stop();
    }

    @Override
    public void finish() {
        super.finish();
        NearbyConnectionsManager.getInstance(getApplicationContext()).disconnectFromEndpoint(masterId);
        currPartIdx = 0;
    }


    public void onDisconnect(View view) {
        WorkStatus workStatus = new WorkStatus();
        workStatus.setPartitionIndexInfo(currPartIdx);
        workStatus.setStatusInfo(Constants.WorkStatus.DISCONNECTED);

        C_Workload tPayload1 = new C_Workload();
        tPayload1.setTag(Constants.PayloadTags.WORK_STATUS);
        tPayload1.setData(workStatus);

        DataShare.transferWorkload(getApplicationContext(), masterId, tPayload1);
        goBack();
    }

    public void beginTask(Payload payload) {
        WorkStatus workStatus = new WorkStatus();
        C_Workload sendPayload = new C_Workload();
        sendPayload.setTag(Constants.PayloadTags.WORK_STATUS);

        try {
            C_Workload receivedPayload = WorkloadConvert.fromPayload(payload);
            if (receivedPayload.getTag().equals(Constants.PayloadTags.WORK_DATA)) {
                connectionInfo("Working now", true);

                TaskInfo workData = (TaskInfo) receivedPayload.getData();
                int dotProduct = Matrixstruct.calcDotProduct(workData.getRows(), workData.getCols());

                Log.d("WORKER_COMPUTATION", "Partition Index: " + workData.getPartIdx());
                if (!completedTask.contains(workData.getPartIdx())) {
                    completedTask.add(workData.getPartIdx());
                }
                currPartIdx = workData.getPartIdx();

                setPartView(completedTask.size());
                workStatus.setPartitionIndexInfo(workData.getPartIdx());
                workStatus.setResultInfo(dotProduct);

                workStatus.setStatusInfo(Constants.WorkStatus.WORKING);
                sendPayload.setData(workStatus);
                DataShare.transferWorkload(getApplicationContext(), masterId, sendPayload);

            } else if (receivedPayload.getTag().equals(Constants.PayloadTags.FAREWELL)) {
                // end measuring energy level
                finalEnergyWorker =
                        mBatteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER);
                energyConsumedWorker = Math.abs(initialEnergyWorker-finalEnergyWorker);
                onTaskCompleted("Work Done !!");
                Log.d("WORKER_COMPUTATION", "Work Done");
                workStatus.setStatusInfo(Constants.WorkStatus.FINISHED);
                sendPayload.setData(workStatus);
                DataShare.transferWorkload(getApplicationContext(), masterId, sendPayload);
                deviceStatsPublisher.stop();

            } else if (receivedPayload.getTag().equals(Constants.PayloadTags.DISCONNECTED)) {
                goBack();
            }

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }


}

