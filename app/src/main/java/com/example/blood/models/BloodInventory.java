package com.example.blood.models;

public class BloodInventory {
    private String bloodType;
    private int units;
    private String status;
    private long lastUpdated;

    public BloodInventory() {
        // Default constructor required for calls to DataSnapshot.getValue(BloodInventory.class)
    }

    public BloodInventory(String bloodType, int units, String status) {
        this.bloodType = bloodType;
        this.units = units;
        this.status = status;
        this.lastUpdated = System.currentTimeMillis();
    }

    public String getBloodType() {
        return bloodType;
    }

    public int getUnits() {
        return units;
    }

    public String getStatus() {
        return status;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    // Setters
    public void setBloodType(String bloodType) {
        this.bloodType = bloodType;
    }

    public void setUnits(int units) {
        this.units = units;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
