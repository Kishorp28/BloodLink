package com.example.blood.firebase;

import android.util.Log;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.example.blood.models.BloodInventory;
import com.example.blood.models.BloodRequest;
import com.example.blood.models.BloodDonation;
import com.example.blood.models.UserProfile;

public class FirebaseManager {
    private static final String TAG = "FirebaseManager";
    private static final String USERS = "users";
    private static final String INVENTORY = "inventory";
    private static final String BLOOD_REQUESTS = "blood_requests";
    private static final String BLOOD_DONATIONS = "blood_donations";
    private static final String BLOOD_BANKS = "blood_banks";
    
    private DatabaseReference database;
    
    public FirebaseManager() {
        this.database = FirebaseDatabase.getInstance().getReference();
    }
    
    // ==================== USER PROFILE OPERATIONS ====================
    
    /**
     * Create or update user profile
     */
    public void saveUserProfile(String userId, UserProfile profile, OnCompleteListener listener) {
        profile.setUserId(userId);
        database.child(USERS).child(userId).setValue(profile, (error, ref) -> {
            if (error == null) {
                listener.onSuccess("User profile saved successfully");
            } else {
                listener.onError(error.getMessage());
            }
        });
    }
    
    /**
     * Get user profile by ID
     */
    public void getUserProfile(String userId, OnDataReceiveListener<UserProfile> listener) {
        database.child(USERS).child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    UserProfile profile = snapshot.getValue(UserProfile.class);
                    listener.onSuccess(profile);
                } else {
                    listener.onError("User profile not found");
                }
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                listener.onError(error.getMessage());
            }
        });
    }
    
    /**
     * Update user profile fields
     */
    public void updateUserProfile(String userId, String field, Object value, OnCompleteListener listener) {
        database.child(USERS).child(userId).child(field).setValue(value, (error, ref) -> {
            if (error == null) {
                listener.onSuccess("User profile updated");
            } else {
                listener.onError(error.getMessage());
            }
        });
    }
    
    // ==================== INVENTORY OPERATIONS ====================
    
    /**
     * Initialize blood inventory for all blood types
     */
    public void initializeInventory() {
        String[] bloodTypes = {"O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-"};
        for (String type : bloodTypes) {
            BloodInventory inventory = new BloodInventory(type, 0, "CRITICAL");
            database.child(INVENTORY).child(type).setValue(inventory);
        }
    }
    
    /**
     * Get blood inventory for a specific blood type
     */
    public void getBloodInventory(String bloodType, OnDataReceiveListener<BloodInventory> listener) {
        database.child(INVENTORY).child(bloodType).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    BloodInventory inventory = snapshot.getValue(BloodInventory.class);
                    listener.onSuccess(inventory);
                } else {
                    listener.onError("Inventory not found for blood type: " + bloodType);
                }
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                listener.onError(error.getMessage());
            }
        });
    }
    
    /**
     * Get all blood inventory
     */
    public void getAllBloodInventory(OnDataReceiveListener<DataSnapshot> listener) {
        database.child(INVENTORY).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                listener.onSuccess(snapshot);
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                listener.onError(error.getMessage());
            }
        });
    }
    
    /**
     * Manually update inventory units
     */
    public void updateInventoryUnits(String bloodType, int units, OnCompleteListener listener) {
        database.child(INVENTORY).child(bloodType).child("units").setValue(units, (error, ref) -> {
            if (error == null) {
                updateInventoryStatus(bloodType, units);
                listener.onSuccess("Inventory updated");
            } else {
                listener.onError(error.getMessage());
            }
        });
    }
    
    /**
     * Update inventory status based on units
     */
    private void updateInventoryStatus(String bloodType, int units) {
        String status;
        if (units >= 10) {
            status = "AVAILABLE";
        } else if (units >= 5) {
            status = "LOW";
        } else {
            status = "CRITICAL";
        }
        
        database.child(INVENTORY).child(bloodType).child("status").setValue(status);
        database.child(INVENTORY).child(bloodType).child("lastUpdated").setValue(System.currentTimeMillis());
    }
    
    // ==================== BLOOD BANK OPERATIONS ====================
    
    /**
     * Get all blood banks in a specific city
     */
    public void getBloodBanks(String city, OnDataReceiveListener<DataSnapshot> listener) {
        database.child(BLOOD_BANKS).orderByChild("city").equalTo(city)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        listener.onSuccess(snapshot);
                    }
                    
                    @Override
                    public void onCancelled(DatabaseError error) {
                        listener.onError(error.getMessage());
                    }
                });
    }

    /**
     * Get all blood banks
     */
    public void getAllBloodBanks(OnDataReceiveListener<DataSnapshot> listener) {
        database.child(BLOOD_BANKS).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                listener.onSuccess(snapshot);
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                listener.onError(error.getMessage());
            }
        });
    }

    /**
     * Initialize Coimbatore Blood Bank data
     */
    public void initializeCoimbatoreData() {
        // Purge old potentially broken data first
        database.child(BLOOD_BANKS).removeValue();
        
        String city = "Coimbatore";
        
        // 1. PSG Hospitals
        com.example.blood.models.BloodBank psg = new com.example.blood.models.BloodBank(
                "PSG Hospitals", "Avinashi Rd, Peelamedu, Coimbatore", city, 11.0263, 77.0016);
        psg.updateStock("A+", 12);
        psg.updateStock("O+", 2);
        psg.updateStock("A-", 5);
        psg.updateStock("B+", 8);
        
        // 2. KG Hospital
        com.example.blood.models.BloodBank kg = new com.example.blood.models.BloodBank(
                "KG Hospital", "Arts College Road, Coimbatore", city, 11.0004, 76.9654);
        kg.updateStock("A+", 4);
        kg.updateStock("B+", 24);
        kg.updateStock("B-", 12);
        kg.updateStock("O-", 3);
        
        // 3. Ganga Hospital
        com.example.blood.models.BloodBank ganga = new com.example.blood.models.BloodBank(
                "Ganga Hospital", "Mettupalayam Road, Coimbatore", city, 11.0205, 76.9534);
        ganga.updateStock("O+", 15);
        ganga.updateStock("AB+", 7);
        ganga.updateStock("A+", 10);
        
        // 4. Sri Ramakrishna Hospital
        com.example.blood.models.BloodBank ramakrishna = new com.example.blood.models.BloodBank(
                "Sri Ramakrishna Hospital", "Saravanampatty, Coimbatore", city, 11.0664, 76.9922);
        ramakrishna.updateStock("B+", 18);
        ramakrishna.updateStock("O+", 5);
        ramakrishna.updateStock("A+", 9);

        // 5. CMCH
        com.example.blood.models.BloodBank cmch = new com.example.blood.models.BloodBank(
                "Coimbatore Medical College Hospital", "Trichy Road, Coimbatore", city, 11.0014, 76.9734);
        cmch.updateStock("O+", 40);
        cmch.updateStock("A+", 30);
        cmch.updateStock("B+", 35);
        cmch.updateStock("AB+", 10);

        // Save to Firebase
        saveBloodBank(psg);
        saveBloodBank(kg);
        saveBloodBank(ganga);
        saveBloodBank(ramakrishna);
        saveBloodBank(cmch);
    }

    private void saveBloodBank(com.example.blood.models.BloodBank bank) {
        // Use sanitized name as ID so it updates existing entries instead of duplicating
        String safeId = bank.getName().replaceAll("[^a-zA-Z0-9]", "_");
        bank.setId(safeId);
        database.child(BLOOD_BANKS).child(safeId).setValue(bank);
    }

    /**
     * Seed sample requests for testing blood type compatibility
     */
    public void seedSampleRequests() {
        String[] types = {"O-", "O+", "A+", "B+", "AB-", "AB+"};
        String[] hospitals = {"Coimbatore Medical College Hospital", "KG Hospital", "Ganga Hospital"};
        
        for (String type : types) {
            String id = "test_req_" + type.replace("+", "p").replace("-", "m");
            BloodRequest req = new BloodRequest(type, 2, hospitals[(int)(Math.random()*hospitals.length)], "Coimbatore", "Emergency donation needed", "URGENT");
            req.setRequestId(id);
            database.child(BLOOD_REQUESTS).child(id).setValue(req);
        }
    }

    // ==================== BLOOD REQUEST OPERATIONS ====================
    
    /**
     * Create a new blood request
     */
    public void createBloodRequest(BloodRequest request, OnCompleteListener listener) {
        String requestId = database.child(BLOOD_REQUESTS).push().getKey();
        request.setRequestId(requestId);
        
        database.child(BLOOD_REQUESTS).child(requestId).setValue(request, (error, ref) -> {
            if (error == null) {
                listener.onSuccess("Blood request created with ID: " + requestId);
            } else {
                listener.onError(error.getMessage());
            }
        });
    }
    
    /**
     * Get all active blood requests
     */
    public void getActiveBloodRequests(OnDataReceiveListener<DataSnapshot> listener) {
        database.child(BLOOD_REQUESTS).orderByChild("status").equalTo("PENDING")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        listener.onSuccess(snapshot);
                    }
                    
                    @Override
                    public void onCancelled(DatabaseError error) {
                        listener.onError(error.getMessage());
                    }
                });
    }
    
    /**
     * Get blood request by ID
     */
    public void getBloodRequest(String requestId, OnDataReceiveListener<BloodRequest> listener) {
        database.child(BLOOD_REQUESTS).child(requestId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    BloodRequest request = snapshot.getValue(BloodRequest.class);
                    listener.onSuccess(request);
                } else {
                    listener.onError("Blood request not found");
                }
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                listener.onError(error.getMessage());
            }
        });
    }
    
    /**
     * Update blood request status
     */
    public void updateBloodRequestStatus(String requestId, String status, OnCompleteListener listener) {
        database.child(BLOOD_REQUESTS).child(requestId).child("status").setValue(status, (error, ref) -> {
            if (error == null) {
                listener.onSuccess("Request status updated");
            } else {
                listener.onError(error.getMessage());
            }
        });
    }
    
    /**
     * Get blood requests by blood type
     */
    public void getBloodRequestsByType(String bloodType, OnDataReceiveListener<DataSnapshot> listener) {
        database.child(BLOOD_REQUESTS).orderByChild("bloodType").equalTo(bloodType)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        listener.onSuccess(snapshot);
                    }
                    
                    @Override
                    public void onCancelled(DatabaseError error) {
                        listener.onError(error.getMessage());
                    }
                });
    }
    
    // ==================== BLOOD DONATION OPERATIONS ====================
    
    /**
     * Record a new blood donation
     * This automatically updates the inventory
     */
    public void recordBloodDonation(BloodDonation donation, OnCompleteListener listener) {
        Log.d(TAG, "=== SAVING DONATION TO FIREBASE ===");
        String donationId = database.child(BLOOD_DONATIONS).push().getKey();
        donation.setDonationId(donationId);
        
        Log.d(TAG, "Donation ID: " + donationId);
        Log.d(TAG, "Donor ID: " + donation.getDonorId());
        Log.d(TAG, "Blood Type: " + donation.getBloodType());
        Log.d(TAG, "Units: " + donation.getUnits());
        
        database.child(BLOOD_DONATIONS).child(donationId).setValue(donation, (error, ref) -> {
            if (error == null) {
                Log.d(TAG, "✓ Donation saved to database at /blood_donations/" + donationId);
                // Automatically update inventory when donation is recorded
                updateInventoryFromDonation(donation, listener, donationId);
            } else {
                Log.e(TAG, "✗ Firebase error saving donation: " + error.getMessage());
                listener.onError("Database error: " + error.getMessage());
            }
        });
    }
    
    /**
     * Update inventory automatically when blood is donated
     * This is called after a donation is recorded
     */
    private void updateInventoryFromDonation(BloodDonation donation, OnCompleteListener listener, String donationId) {
        // Get current inventory
        database.child(INVENTORY).child(donation.getBloodType()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    BloodInventory inventory = snapshot.getValue(BloodInventory.class);
                    if (inventory != null) {
                        // Add donated units to inventory
                        int newUnits = inventory.getUnits() + donation.getUnits();
                        
                        // Update inventory
                        database.child(INVENTORY).child(donation.getBloodType()).child("units").setValue(newUnits);
                        
                        // Update status
                        updateInventoryStatus(donation.getBloodType(), newUnits);
                        
                        // Update user's donation history
                        addDonationToUserHistory(donation.getDonorId(), donationId, 
                                                donation.getBloodType(), donation.getUnits());
                        
                        listener.onSuccess("Donation recorded and inventory updated! Added " + donation.getUnits() + " units of " + donation.getBloodType());
                    }
                } else {
                    listener.onError("Inventory not found for blood type: " + donation.getBloodType());
                }
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                listener.onError("Failed to update inventory: " + error.getMessage());
            }
        });
    }
    
    /**
     * Add donation to user's donation history
     */
    private void addDonationToUserHistory(String userId, String donationId, String bloodType, int units) {
        UserProfile.DonationHistoryItem historyItem = new UserProfile.DonationHistoryItem(
                donationId,
                System.currentTimeMillis(),
                "",
                units,
                "COMPLETED"
        );
        
        database.child(USERS).child(userId).child("donationHistory").child(donationId).setValue(historyItem);
        database.child(USERS).child(userId).child("lastDonationDate").setValue(System.currentTimeMillis());
        database.child(USERS).child(userId).child("totalDonations").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Integer currentCount = snapshot.getValue(Integer.class);
                int newCount = (currentCount != null ? currentCount : 0) + 1;
                database.child(USERS).child(userId).child("totalDonations").setValue(newCount);
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
            }
        });
    }
    
    /**
     * Get donation by ID
     */
    public void getBloodDonation(String donationId, OnDataReceiveListener<BloodDonation> listener) {
        database.child(BLOOD_DONATIONS).child(donationId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    BloodDonation donation = snapshot.getValue(BloodDonation.class);
                    listener.onSuccess(donation);
                } else {
                    listener.onError("Donation not found");
                }
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                listener.onError(error.getMessage());
            }
        });
    }
    
    /**
     * Get all donations by a specific donor
     */
    public void getDonorDonations(String donorId, OnDataReceiveListener<DataSnapshot> listener) {
        database.child(BLOOD_DONATIONS).orderByChild("donorId").equalTo(donorId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        listener.onSuccess(snapshot);
                    }
                    
                    @Override
                    public void onCancelled(DatabaseError error) {
                        listener.onError(error.getMessage());
                    }
                });
    }
    
    /**
     * Update donation status (when verified by admin)
     */
    public void updateDonationStatus(String donationId, String status, String verifiedBy, OnCompleteListener listener) {
        database.child(BLOOD_DONATIONS).child(donationId).child("status").setValue(status);
        database.child(BLOOD_DONATIONS).child(donationId).child("verifiedBy").setValue(verifiedBy);
        database.child(BLOOD_DONATIONS).child(donationId).child("verificationDate").setValue(System.currentTimeMillis(), (error, ref) -> {
            if (error == null) {
                listener.onSuccess("Donation status updated");
            } else {
                listener.onError(error.getMessage());
            }
        });
    }
    
    // ==================== LISTENER INTERFACES ====================
    
    public interface OnCompleteListener {
        void onSuccess(String message);
        void onError(String error);
    }
    
    public interface OnDataReceiveListener<T> {
        void onSuccess(T data);
        void onError(String error);
    }
}
