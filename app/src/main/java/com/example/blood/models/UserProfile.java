package com.example.blood.models;

import java.util.HashMap;
import java.util.Map;

public class UserProfile {
    private String userId;
    private String name;
    private String email;
    private String phone;
    private String bloodType;
    private String address;
    private String city;
    private double latitude;
    private double longitude;
    private long registrationDate;
    private String profilePhotoUrl;
    private boolean isDonor;
    private int totalDonations;
    private long lastDonationDate;
    private Map<String, DonationHistoryItem> donationHistory;
    
    public UserProfile() {
        this.donationHistory = new HashMap<>();
    }
    
    public UserProfile(String name, String email, String phone, String bloodType) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.bloodType = bloodType;
        this.registrationDate = System.currentTimeMillis();
        this.isDonor = true;
        this.totalDonations = 0;
        this.donationHistory = new HashMap<>();
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getBloodType() {
        return bloodType;
    }
    
    public void setBloodType(String bloodType) {
        this.bloodType = bloodType;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public String getCity() {
        return city;
    }
    
    public void setCity(String city) {
        this.city = city;
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
    
    public long getRegistrationDate() {
        return registrationDate;
    }
    
    public void setRegistrationDate(long registrationDate) {
        this.registrationDate = registrationDate;
    }
    
    public String getProfilePhotoUrl() {
        return profilePhotoUrl;
    }
    
    public void setProfilePhotoUrl(String profilePhotoUrl) {
        this.profilePhotoUrl = profilePhotoUrl;
    }
    
    public boolean isDonor() {
        return isDonor;
    }
    
    public void setDonor(boolean donor) {
        isDonor = donor;
    }
    
    public int getTotalDonations() {
        return totalDonations;
    }
    
    public void setTotalDonations(int totalDonations) {
        this.totalDonations = totalDonations;
    }
    
    public long getLastDonationDate() {
        return lastDonationDate;
    }
    
    public void setLastDonationDate(long lastDonationDate) {
        this.lastDonationDate = lastDonationDate;
    }
    
    public Map<String, DonationHistoryItem> getDonationHistory() {
        return donationHistory;
    }
    
    public void setDonationHistory(Map<String, DonationHistoryItem> donationHistory) {
        this.donationHistory = donationHistory;
    }
    
    // Inner class for donation history
    public static class DonationHistoryItem {
        public String donationId;
        public long donationDate;
        public String center;
        public int units;
        public String status;
        
        public DonationHistoryItem() {
        }
        
        public DonationHistoryItem(String donationId, long donationDate, 
                                   String center, int units, String status) {
            this.donationId = donationId;
            this.donationDate = donationDate;
            this.center = center;
            this.units = units;
            this.status = status;
        }
    }
}
