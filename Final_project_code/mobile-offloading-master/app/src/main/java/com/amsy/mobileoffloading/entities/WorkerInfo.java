package com.amsy.mobileoffloading.entities;

import android.location.Location;

import java.io.Serializable;

public class WorkerInfo implements Serializable {

    private int potential;
    private int chargeLevel;
    private boolean power;

    private double latitude;
    private double longitude;

    private boolean locCheck;

    public int getChargeLevel() {
        return chargeLevel;
    }

    public void setChargeLevel(int chargeLevel) {
        this.chargeLevel = chargeLevel;
    }

    public boolean isPower() {
        return power;
    }

    public void setPower(boolean power) {
        this.power = power;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public boolean isLocCheck() {
        return locCheck;
    }

    public void setLocCheck(boolean locCheck) {
        this.locCheck = locCheck;
    }

    public int getPotential() {
        return potential;
    }

    public void setPotential(int potential) {
        this.potential = potential;
    }

    public void setLocation(Location loc) {
        if(loc != null) {
            this.latitude = loc.getLatitude();
            this.longitude = loc.getLongitude();
            this.locCheck = true;
        } else {
            this.locCheck = false;
        }
    }
}
