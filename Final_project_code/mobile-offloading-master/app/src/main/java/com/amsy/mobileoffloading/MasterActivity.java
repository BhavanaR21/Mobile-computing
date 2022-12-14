package com.amsy.mobileoffloading;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import android.content.Context;
import android.location.Location;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import com.amsy.mobileoffloading.adapters.WorkersView;
import com.amsy.mobileoffloading.callback.WorkerUpdateListener;
import com.amsy.mobileoffloading.entities.LinkedDevices;
import com.amsy.mobileoffloading.entities.WorkerInfo;
import com.amsy.mobileoffloading.entities.WorkStatus;
import com.amsy.mobileoffloading.entities.Worker;
import com.amsy.mobileoffloading.helper.Constants;
import com.amsy.mobileoffloading.helper.TransferToFile;
import com.amsy.mobileoffloading.helper.Matrixstruct;
import com.amsy.mobileoffloading.services.DeviceStatisticsPublisher;
import com.amsy.mobileoffloading.services.NearbyConnectionsManager;
import com.amsy.mobileoffloading.services.WorkAllocator;
import com.amsy.mobileoffloading.services.WorkerStatusSubscriber;
import com.app.progresviews.ProgressWheel;

import java.util.ArrayList;
import java.util.HashMap;

import needle.Needle;

public class MasterActivity extends AppCompatActivity {

    private RecyclerView rvWorkers;

    private HashMap<String, WorkerStatusSubscriber> workerStatusSubscriberMap = new HashMap<>();

    private ArrayList<Worker> workers = new ArrayList<>();
    private WorkersView workersView;


    /* [row1 x cols1] * [row2 * cols2] */
    private int rows1 = Constants.matrix_rows;
    private int cols1 = Constants.matrix_columns;
    private int rows2 = Constants.matrix_columns;
    private int cols2 = Constants.matrix_rows;

    private int[][] matrix1;
    private int[][] matrix2;

    private WorkAllocator workAllocator;

    private int workAmount;
    private int totalPartitions;
    private Handler handler;
    private Runnable runnable;
    private DeviceStatisticsPublisher deviceStatsPublisher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master);

        Log.d("MasterDiscovery", "Starting computing matrix multiplication on only master");
        TextView masterPower = findViewById(R.id.masterPower);
        masterPower.setText("Stats not available");
        BatteryManager mBatteryManager =
                (BatteryManager)getSystemService(Context.BATTERY_SERVICE);
        Long initialEnergyMaster =
                mBatteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER);
        computeMatrixMultiplicationOnMaster();
        Long finalEnergyMaster =
                mBatteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER);
        Long energyConsumedMaster = Math.abs(initialEnergyMaster-finalEnergyMaster);
        masterPower.setText("Power Consumption (Master): " +Long.toString(energyConsumedMaster)+ " nWh");
        Log.d("MasterDiscovery", "Completed computing matrix multiplication on only master");

        unpackBundle();
        bindViews();
        setAdapters();
        init();
        setupDeviceBatteryStatsCollector();

    }

    @Override
    protected void onPause() {
        super.onPause();
        stopWorkerStatusSubscribers();
        deviceStatsPublisher.stop();
        handler.removeCallbacks(runnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startWorkerStatusSubscribers();
        deviceStatsPublisher.start();
        handler.postDelayed(runnable, Constants.UPDATE_INTERVAL_UI);
    }

    @Override
    public void onBackPressed() {
        for (Worker w : workers) {
            updateWorkerConnectionStatus(w.getEndpointId(), Constants.WorkStatus.DISCONNECTED);
            workAllocator.removeWorker(w.getEndpointId());
            NearbyConnectionsManager.getInstance(getApplicationContext()).disconnectFromEndpoint(w.getEndpointId());
        }
        super.onBackPressed();
        finish();
    }

    private void init() {
        totalPartitions = rows1 * cols2;
        updateProgress(0);
        matrix1 = Matrixstruct.createMatrix(rows1, cols1);
        matrix2 = Matrixstruct.createMatrix(rows2, cols2);

        workAllocator = new WorkAllocator(getApplicationContext(), workers, matrix1, matrix2, slaveTime -> {
            TextView slave = findViewById(R.id.slaveTime);
            slave.setText("Execution time (Slave): " + slaveTime + "ms");
        });
        workAllocator.beginDistributedComputation();
    }

    private void updateProgress(int done) {
        ProgressWheel wheel = findViewById(R.id.wheelprogress);
        int per = 360 * done / totalPartitions;
        wheel.setPercentage(per);
        wheel.setStepCountText(done + "");
        TextView totalPart = findViewById(R.id.totalPartitions);
        totalPart.setText("Total Partitions: " + totalPartitions);
        if(per == 360) {
            deviceStatsPublisher.stop();
        }
    }

    private void bindViews() {
        rvWorkers = findViewById(R.id.rv_workers);
        SimpleItemAnimator itemAnimator = (SimpleItemAnimator) rvWorkers.getItemAnimator();
        itemAnimator.setSupportsChangeAnimations(false);
    }


    private void setAdapters() {
        workersView = new WorkersView(this, workers);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        rvWorkers.setLayoutManager(linearLayoutManager);

        rvWorkers.setAdapter(workersView);
        workersView.notifyDataSetChanged();
    }


    private void unpackBundle() {
        try {
            Bundle bundle = getIntent().getExtras();

            ArrayList<LinkedDevices> linkedDevices = (ArrayList<LinkedDevices>) bundle.getSerializable(Constants.CONNECTED_DEVICES);
            addToWorkers(linkedDevices);
            Log.d("CHECK", "Added a connected Device as worker");
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    private void addToWorkers(ArrayList<LinkedDevices> linkedDevices) {
        for (LinkedDevices LinkedDevice : linkedDevices) {
            Worker worker = new Worker();
            worker.setEndpointId(LinkedDevice.getConnectID());
            worker.setEndpointName(LinkedDevice.getConnectName());

            WorkStatus workStatus = new WorkStatus();
            workStatus.setStatusInfo(Constants.WorkStatus.WORKING);

            worker.setWorkStatus(workStatus);
            worker.setDeviceStats(new WorkerInfo());

            workers.add(worker);
        }
    }

    private void computeMatrixMultiplicationOnMaster() {
        matrix1 = Matrixstruct.createMatrix(rows1, cols1);
        matrix2 = Matrixstruct.createMatrix(rows2, cols2);
        Needle.onBackgroundThread().execute(() -> {
            long startTime = System.currentTimeMillis();
            int[][] mul = new int[rows1][cols2];
            for (int i = 0; i < rows1; i++) {
                for (int j = 0; j < cols2; j++) {
                    mul[i][j] = 0;
                    for (int k = 0; k < cols1; k++) {
                        mul[i][j] += matrix1[i][k] * matrix2[k][j];
                    }
                }
            }
            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            TransferToFile.sendTextToFile(getApplicationContext(), "exec_time_master_alone.txt", false, totalTime + "ms");
            TextView master = findViewById(R.id.masterTime);
            master.setText("Execution time (Master): " + totalTime + "ms");
        });
    }



    private void setupDeviceBatteryStatsCollector() {
        deviceStatsPublisher = new DeviceStatisticsPublisher(getApplicationContext(), null, Constants.UPDATE_INTERVAL_UI);
        handler = new Handler();
        runnable = () -> {
            String deviceStatsStr = DeviceStatisticsPublisher.getBatteryLevel(this) + "%"
                    + "\t" + (DeviceStatisticsPublisher.isPluggedIn(this) ? "CHARGING" : "NOT CHARGING");
            TransferToFile.sendTextToFile(getApplicationContext(), "master_battery.txt", true, deviceStatsStr);
            handler.postDelayed(runnable, Constants.UPDATE_INTERVAL_UI);
        };
    }


    private void updateWorkerConnectionStatus(String endpointId, String status) {
        Log.d("DISCONNECTED----", endpointId);
        for (int i = 0; i < workers.size(); i++) {

            Log.d("DISCONNECTED--", workers.get(i).getEndpointId());
            if (workers.get(i).getEndpointId().equals(endpointId)) {
                workers.get(i).getWorkStatus().setStatusInfo(status);
                workersView.notifyDataSetChanged();
                break;
            }
        }
    }


    private void startWorkerStatusSubscribers() {
        for (Worker worker : workers) {
            if (workerStatusSubscriberMap.containsKey(worker.getEndpointId())) {
                continue;
            }

            WorkerStatusSubscriber workerStatusSubscriber = new WorkerStatusSubscriber(getApplicationContext(), worker.getEndpointId(), new WorkerUpdateListener() {
                @Override
                public void onWorkStatusReceived(String endpointId, WorkStatus workStatus) {

                    if (workStatus.getStatusInfo().equals(Constants.WorkStatus.DISCONNECTED)) {
                        updateWorkerConnectionStatus(endpointId, Constants.WorkStatus.DISCONNECTED);
                        workAllocator.removeWorker(endpointId);
                        NearbyConnectionsManager.getInstance(getApplicationContext()).rejectConnection(endpointId);
                    } else {
                        updateWorkerStatus(endpointId, workStatus);
                    }
                    workAllocator.checkWorkCompletion(getWorkAmount());
                }

                @Override
                public void onDeviceStatsReceived(String endpointId, WorkerInfo deviceStats) {
                    updateWorkerStatus(endpointId, deviceStats);

                    String deviceStatsStr = deviceStats.getChargeLevel() + "%"
                            + "\t" + (deviceStats.isPower() ? "CHARGING" : "NOT CHARGING")
                            + "\t\t" + deviceStats.getLatitude()
                            + "\t" + deviceStats.getLongitude();
                    TransferToFile.sendTextToFile(getApplicationContext(), endpointId + ".txt", true, deviceStatsStr);
                    Log.d("MASTER_ACTIVITY", "WORK AMOUNT: " + getWorkAmount());
                    workAllocator.checkWorkCompletion(getWorkAmount());
                }
            });

            workerStatusSubscriber.start();
            workerStatusSubscriberMap.put(worker.getEndpointId(), workerStatusSubscriber);
        }
    }


    private int getWorkAmount() {
        int sum = 0;
        for (Worker worker : workers) {
            sum += worker.getWorkAmount();

        }
        return sum;
    }

    private void updateWorkerStatus(String endpointId, WorkStatus workStatus) {
        for (int i = 0; i < workers.size(); i++) {
            Worker worker = workers.get(i);

            if (worker.getEndpointId().equals(endpointId)) {
                worker.setWorkStatus(workStatus);

                if (workStatus.getStatusInfo().equals(Constants.WorkStatus.WORKING) && workAllocator.isItNewWork(workStatus.getPartitionIndexInfo())) {
                    workers.get(i).setWorkAmount(workers.get(i).getWorkAmount() + 1);
                    workAmount += 1;
                }

                workAllocator.updateWorkStatus(worker, workStatus);

                workersView.notifyItemChanged(i);
                break;
            }
        }
        updateProgress(workAmount);
    }

    private void updateWorkerStatus(String endpointId, WorkerInfo deviceStats) {
        for (int i = 0; i < workers.size(); i++) {
            Worker worker = workers.get(i);

            if (worker.getEndpointId().equals(endpointId)) {
                worker.setDeviceStats(deviceStats);
                Location masterLocation = DeviceStatisticsPublisher.getLocation(this);
                if (deviceStats.isLocCheck() && masterLocation != null) {
                    float[] results = new float[1];
                    Location.distanceBetween(masterLocation.getLatitude(), masterLocation.getLongitude(),
                            deviceStats.getLatitude(), deviceStats.getLongitude(), results);
                    Log.d("MASTER_ACTIVITY", "Master Location: " + masterLocation.getLatitude() + ", " + masterLocation.getLongitude());
                    Log.d("MASTER_ACTIVITY", "Master Distance: " + results[0]);
                    worker.setDistanceFromMaster(results[0]);
                }

                workersView.notifyItemChanged(i);
            }
        }
    }

    private void stopWorkerStatusSubscribers() {
        for (Worker worker : workers) {
            WorkerStatusSubscriber workerStatusSubscriber = workerStatusSubscriberMap.get(worker.getEndpointId());
            if (workerStatusSubscriber != null) {
                workerStatusSubscriber.stop();
                workerStatusSubscriberMap.remove(worker.getEndpointId());
            }
        }
    }


}