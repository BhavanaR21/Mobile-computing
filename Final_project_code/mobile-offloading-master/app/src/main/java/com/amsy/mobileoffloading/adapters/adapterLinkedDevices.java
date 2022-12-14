package com.amsy.mobileoffloading.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.amsy.mobileoffloading.R;
import com.amsy.mobileoffloading.entities.LinkedDevices;
import com.amsy.mobileoffloading.helper.Constants;

import java.util.List;

public class adapterLinkedDevices extends RecyclerView.Adapter<adapterLinkedDevices.ViewHolder>{

    private Context context;
    private List<LinkedDevices> linkedDevices;

    public adapterLinkedDevices(@NonNull Context context, List<LinkedDevices> linkedDevices) {
        this.context = context;
        this.linkedDevices = linkedDevices;
    }

    @NonNull
    @Override
    public adapterLinkedDevices.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View itemView = layoutInflater.inflate(R.layout.connected_device_item, parent, false);

        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull adapterLinkedDevices.ViewHolder holder, int position) {
        holder.SetC_ID(linkedDevices.get(position).getConnectID(), linkedDevices.get(position).getConnectName());
        holder.BatteryStatus(linkedDevices.get(position).getDeviceInfo().getChargeLevel());
        holder.setRequestStatus(linkedDevices.get(position).getPermissionStatus());
    }

    @Override
    public int getItemCount() {
        return linkedDevices.size();
    }

    protected class ViewHolder extends RecyclerView.ViewHolder {

        private TextView C_ID;
        private TextView Charge;
        private ImageView PermissionStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            C_ID = itemView.findViewById(R.id.WorkerId);
            Charge = itemView.findViewById(R.id.ChargeID);
            PermissionStatus = itemView.findViewById(R.id.PermissionID);
        }

        public void SetC_ID(String connectID, String connectName) {
            this.C_ID.setText(connectName.toUpperCase()  + "\n(" + connectID.toUpperCase() + ")");
        }

        public void BatteryStatus(int charge) {
            if (charge > 0 && charge <= 100) {
                this.Charge.setText(charge + "%");
            } else {
                this.Charge.setText("");
            }
        }

        public void setRequestStatus(String requestStatus) {
            if (requestStatus.equals(Constants.RequestStatus.ACCEPTED)) {
                this.PermissionStatus.setBackgroundResource(R.drawable.ic_outline_check_circle_24);
            } else if (requestStatus.equals(Constants.RequestStatus.REJECTED)) {
                this.PermissionStatus.setBackgroundResource(R.drawable.ic_outline_cancel_24);
            } else {
                this.PermissionStatus.setBackgroundResource(R.drawable.ic_outline_pending_24);
            }
        }
    }
}
