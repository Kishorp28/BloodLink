package com.example.blood.models;

public class BloodDonation {
    private String donationId;
    private String donorId;
    private String bloodType;
    private int units;
    private long donationDate;
    private String donationCenter;
    private String status; // PENDING, VERIFIED, COMPLETED, REJECTED
    private String verifiedBy;
    private long verificationDate;
    private String notes;
    
    public BloodDonation() {
    }
    
    public BloodDonation(String donorId, String bloodType, int units, 
                        String donationCenter) {
        this.donorId = donorId;
        this.bloodType = bloodType;
        this.units = units;
        this.donationCenter = donationCenter;
        this.donationDate = System.currentTimeMillis();
        this.status = "PENDING";
    }
    
    public String getDonationId() {
        return donationId;
    }
    
    public void setDonationId(String donationId) {
        this.donationId = donationId;
    }
    
    public String getDonorId() {
        return donorId;
    }
    
    public void setDonorId(String donorId) {
        this.donorId = donorId;
    }
    
    public String getBloodType() {
        return bloodType;
    }
    
    public void setBloodType(String bloodType) {
        this.bloodType = bloodType;
    }
    
    public int getUnits() {
        return units;
    }
    
    public void setUnits(int units) {
        this.units = units;
    }
    
    public long getDonationDate() {
        return donationDate;
    }
    
    public void setDonationDate(long donationDate) {
        this.donationDate = donationDate;
    }
    
    public String getDonationCenter() {
        return donationCenter;
    }
    
    public void setDonationCenter(String donationCenter) {
        this.donationCenter = donationCenter;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getVerifiedBy() {
        return verifiedBy;
    }
    
    public void setVerifiedBy(String verifiedBy) {
        this.verifiedBy = verifiedBy;
    }
    
    public long getVerificationDate() {
        return verificationDate;
    }
    
    public void setVerificationDate(long verificationDate) {
        this.verificationDate = verificationDate;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
}
