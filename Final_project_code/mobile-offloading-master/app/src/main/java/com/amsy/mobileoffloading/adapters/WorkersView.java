package com.amsy.mobileoffloading.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.amsy.mobileoffloading.R;
import com.amsy.mobileoffloading.entities.Worker;
import com.amsy.mobileoffloading.helper.Constants;

import java.util.List;

import eo.view.batterymeter.BatteryMeterView;

public class WorkersView extends RecyclerView.Adapter<WorkersView.ViewHolder>{
    private Context context;
    private List<Worker> workers;

    public WorkersView(@NonNull Context context, List<Worker> workers) {
        this.context = context;
        this.workers = workers;
    }

    @NonNull

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.setC_ID(workers.get(position).getEndpointId(), workers.get(position).getEndpointName());
        holder.setProgress(workers.get(position).getWorkStatus().getStatusInfo());
        holder.setChargeLevel(workers.get(position).getDeviceStats().getChargeLevel(), workers.get(position).getDeviceStats().isPower());
        holder.setWorkDone(workers.get(position).getWorkAmount());
        holder.setLoc(workers.get(position).getDistanceFromMaster());
    }

    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View itemView = layoutInflater.inflate(R.layout.item_worker, parent, false);

        return new ViewHolder(itemView);
    }



    @Override
    public int getItemCount() {
        return workers.size();
    }

    protected class ViewHolder extends RecyclerView.ViewHolder {

        private TextView viewC_ID;
        private TextView viewProgress;
        private TextView viewChargeLevel;
        private BatteryMeterView viewBatteryStatus;
        private TextView viewWorkStatus;
        private TextView viewLoc;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            viewC_ID = itemView.findViewById(R.id.tv_client_id);
            viewProgress = itemView.findViewById(R.id.tv_work_status);
            viewChargeLevel = itemView.findViewById(R.id.tv_battery_level);
            viewBatteryStatus = itemView.findViewById(R.id.workerBattery);
            viewWorkStatus = itemView.findViewById(R.id.tv_work_finished);
            viewLoc = itemView.findViewById(R.id.tv_location);

        }
        public void setChargeLevel(int batteryLevel, boolean charging) {
            this.viewChargeLevel.setText(batteryLevel + "%");
            this.viewBatteryStatus.setCharging(charging);
            this.viewBatteryStatus.setChargeLevel(batteryLevel);
        }

        public void setWorkDone(int amountWork) {
            viewWorkStatus.setText( amountWork +"");
        }

        public void setProgress(String progress) {
            if (progress.equals(Constants.WorkStatus.WORKING)) {
                this.viewProgress.setText("WORKING...");
            } else if (progress.equals(Constants.WorkStatus.FINISHED)) {
                this.viewProgress.setText("FINISHED");
            } else if (progress.equals(Constants.WorkStatus.FAILED)) {
                this.viewProgress.setText("FAILED");
            } else {
                this.viewProgress.setText("DISCONNECTED");
            }
        }

        public void setC_ID(String endpointId, String endpointName) {
            this.viewC_ID.setText(endpointName.toUpperCase() + " (" + endpointId.toUpperCase() + ")");
        }

        public void setLoc(float distance) {
            viewLoc.setText(String.format("%.2f", distance) + " meters");
        }
    }
}
