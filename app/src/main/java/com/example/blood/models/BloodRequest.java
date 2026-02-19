package com.example.blood.models;

public class BloodRequest {
    private String requestId;
    private String bloodType;
    private int unitsNeeded;
    private String hospitalName;
    private String location;
    private String description;
    private String urgency;
    private String status;
    private long createdTime;
    private long expiryTime;
    private String createdBy;

    public BloodRequest() {
        // Default constructor required for calls to DataSnapshot.getValue(BloodRequest.class)
    }

    public BloodRequest(String bloodType, int unitsNeeded, String hospitalName, String location, String description, String urgency) {
        this.bloodType = bloodType;
        this.unitsNeeded = unitsNeeded;
        this.hospitalName = hospitalName;
        this.location = location;
        this.description = description;
        this.urgency = urgency;
        this.status = "PENDING";  // New requests always start as pending
        this.createdTime = System.currentTimeMillis();
        this.expiryTime = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000); // 7 days
    }

    // Getters
    public String getRequestId() {
        return requestId;
    }

    public String getBloodType() {
        return bloodType;
    }

    public int getUnitsNeeded() {
        return unitsNeeded;
    }

    public String getHospitalName() {
        return hospitalName;
    }

    public String getLocation() {
        return location;
    }

    public String getDescription() {
        return description;
    }

    public String getUrgency() {
        return urgency;
    }

    public String getStatus() {
        return status;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public long getExpiryTime() {
        return expiryTime;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    // Setters
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public void setBloodType(String bloodType) {
        this.bloodType = bloodType;
    }

    public void setUnitsNeeded(int unitsNeeded) {
        this.unitsNeeded = unitsNeeded;
    }

    public void setHospitalName(String hospitalName) {
        this.hospitalName = hospitalName;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setUrgency(String urgency) {
        this.urgency = urgency;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

    public void setExpiryTime(long expiryTime) {
        this.expiryTime = expiryTime;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
