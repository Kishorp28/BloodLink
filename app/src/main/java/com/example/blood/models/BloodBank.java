package com.example.blood.models;

import java.util.HashMap;
import java.util.Map;

public class BloodBank {
    private String id;
    private String name;
    private String address;
    private String city;
    private double latitude;
    private double longitude;
    private Map<String, Integer> inventory;

    public BloodBank() {
        // Required for Firebase
        this.inventory = new HashMap<>();
    }

    public BloodBank(String name, String address, String city, double lat, double lon) {
        this.name = name;
        this.address = address;
        this.city = city;
        this.latitude = lat;
        this.longitude = lon;
        this.inventory = new HashMap<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public Map<String, Integer> getInventory() {
        return inventory;
    }

    public void setInventory(Map<String, Integer> inventory) {
        this.inventory = inventory;
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

    public void updateStock(String bloodType, int units) {
        inventory.put(bloodType, units);
    }
}
