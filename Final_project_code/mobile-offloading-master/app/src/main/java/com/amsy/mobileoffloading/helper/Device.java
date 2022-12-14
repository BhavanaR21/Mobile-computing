package com.amsy.mobileoffloading.helper;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import com.amsy.mobileoffloading.entities.WorkerInfo;

public class Device {

    public static int getCharge(Context context) {
        BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
    }

    public static boolean isConnected(Context context) {
        Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int connStatus = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        return connStatus == BatteryManager.BATTERY_STATUS_CHARGING
                || connStatus == BatteryManager.BATTERY_PLUGGED_AC
                || connStatus == BatteryManager.BATTERY_PLUGGED_USB
                || connStatus == BatteryManager.BATTERY_PLUGGED_WIRELESS;
    }

    public static WorkerInfo getStats(Context context) {
        int charge = Device.getCharge(context);
        boolean charging = Device.isConnected(context);

        WorkerInfo deviceStats = new WorkerInfo();
        deviceStats.setChargeLevel(charge);
        deviceStats.setPower(charging);

        return deviceStats;
    }
}
