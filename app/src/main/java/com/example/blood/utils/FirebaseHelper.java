package com.example.blood.utils;

import android.graphics.Color;
import android.widget.Toast;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.blood.firebase.FirebaseManager;
import com.example.blood.models.BloodDonation;
import com.example.blood.models.BloodRequest;
import com.example.blood.models.UserProfile;
import com.google.firebase.database.DataSnapshot;

/**
 * Firebase Helper class for common operations
 * Use this as reference for implementing Firebase calls in your Activities
 */
public class FirebaseHelper {
    
    private static final String TAG = "FirebaseHelper";
    private FirebaseManager firebaseManager;
    private FirebaseAuth firebaseAuth;
    private AppCompatActivity activity;
    
    public FirebaseHelper(AppCompatActivity activity) {
        this.activity = activity;
        this.firebaseManager = new FirebaseManager();
        this.firebaseAuth = FirebaseAuth.getInstance();
    }
    
    // ==================== USER REGISTRATION & PROFILE ====================
    
    /**
     * Register user and save profile to Firebase
     * Call this after user creates account with Firebase Auth
     */
    public void registerUserProfile(String name, String email, String phone, String bloodType, 
                                   String address, String city) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            UserProfile profile = new UserProfile(name, email, phone, bloodType);
            profile.setAddress(address);
            profile.setCity(city);
            
            firebaseManager.saveUserProfile(currentUser.getUid(), profile, 
                    new FirebaseManager.OnCompleteListener() {
                        @Override
                        public void onSuccess(String message) {
                            showToast("Profile created successfully! Welcome " + name);
                        }
                        
                        @Override
                        public void onError(String error) {
                            showToast("Error: " + error);
                        }
                    });
        }
    }
    
    /**
     * Save user profile directly - with callback listener
     */
    public void saveUserProfile(UserProfile profile, OnCompleteListener listener) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            profile.setUserId(currentUser.getUid());
            firebaseManager.saveUserProfile(currentUser.getUid(), profile, 
                    new FirebaseManager.OnCompleteListener() {
                        @Override
                        public void onSuccess(String message) {
                            listener.onSuccess(message);
                        }
                        
                        @Override
                        public void onError(String error) {
                            listener.onError(error);
                        }
                    });
        } else {
            listener.onError("User not authenticated");
        }
    }
    
    /**
     * Get current user's profile
     */
    public void loadUserProfile(OnProfileLoadedListener listener) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            firebaseManager.getUserProfile(currentUser.getUid(), 
                    new FirebaseManager.OnDataReceiveListener<UserProfile>() {
                        @Override
                        public void onSuccess(UserProfile profile) {
                            listener.onProfileLoaded(profile);
                        }
                        
                        @Override
                        public void onError(String error) {
                            showToast("Error loading profile: " + error);
                            listener.onError(error);
                        }
                    });
        } else {
            showToast("User not authenticated");
            listener.onError("User not authenticated");
        }
    }
    
    /**
     * Update user location (latitude, longitude)
     */
    public void updateUserLocation(double latitude, double longitude) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            firebaseManager.updateUserProfile(currentUser.getUid(), "latitude", latitude, 
                    new FirebaseManager.OnCompleteListener() {
                        @Override
                        public void onSuccess(String message) {
                            firebaseManager.updateUserProfile(currentUser.getUid(), "longitude", longitude,
                                    new FirebaseManager.OnCompleteListener() {
                                        @Override
                                        public void onSuccess(String message) {
                                            showToast("Location updated");
                                        }
                                        
                                        @Override
                                        public void onError(String error) {
                                            showToast("Error: " + error);
                                        }
                                    });
                        }
                        
                        @Override
                        public void onError(String error) {
                            showToast("Error: " + error);
                        }
                    });
        }
    }
    
    // ==================== BLOOD INVENTORY ====================
    
    /**
     * Get all blood inventory and display in dashboard
     */
    public void loadBloodInventory(OnInventoryLoadedListener listener) {
        firebaseManager.getAllBloodInventory(
                new FirebaseManager.OnDataReceiveListener<DataSnapshot>() {
                    @Override
                    public void onSuccess(DataSnapshot snapshot) {
                        listener.onInventoryLoaded(snapshot);
                    }
                    
                    @Override
                    public void onError(String error) {
                        showToast("Error loading inventory: " + error);
                        listener.onError(error);
                    }
                });
    }
    
    /**
     * Get specific blood type inventory
     */
    public void getBloodTypeInventory(String bloodType, OnBloodInventoryListener listener) {
        firebaseManager.getBloodInventory(bloodType, 
                new FirebaseManager.OnDataReceiveListener() {
                    @Override
                    public void onSuccess(Object data) {
                        listener.onSuccess((com.example.blood.models.BloodInventory) data);
                    }
                    
                    @Override
                    public void onError(String error) {
                        showToast("Error: " + error);
                        listener.onError(error);
                    }
                });
    }
    
    // ==================== BLOOD DONATIONS ====================
    
    /**
     * Record blood donation and automatically update inventory
     * This is the KEY method - it handles everything!
     */
    public void recordDonation(String bloodType, int units, String donationCenter) {
        recordDonation(bloodType, units, donationCenter, null);
    }

    /**
     * Record a blood donation with callback listener
     */
    public void recordDonation(String bloodType, int units, String donationCenter, OnCompleteListener listener) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        
        Log.d(TAG, "=== RECORDING DONATION ===");
        Log.d(TAG, "Current User: " + (currentUser != null ? currentUser.getUid() : "NULL"));
        Log.d(TAG, "Blood Type: " + bloodType);
        Log.d(TAG, "Units: " + units);
        Log.d(TAG, "Center: " + donationCenter);
        
        if (currentUser != null) {
            BloodDonation donation = new BloodDonation(currentUser.getUid(), bloodType, units, donationCenter);
            Log.d(TAG, "BloodDonation object created: " + donation.toString());
            
            firebaseManager.recordBloodDonation(donation, 
                    new FirebaseManager.OnCompleteListener() {
                        @Override
                        public void onSuccess(String message) {
                            Log.d(TAG, "✓ Firebase save successful: " + message);
                            showToast("✓ Donation recorded! " + message);
                            // Inventory is automatically updated
                            if (listener != null) {
                                listener.onSuccess(message);
                            }
                        }
                        
                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "✗ Firebase save failed: " + error);
                            showToast("✗ Error recording donation: " + error);
                            if (listener != null) {
                                listener.onError(error);
                            }
                        }
                    });
        } else {
            Log.e(TAG, "✗ USER NOT AUTHENTICATED - Cannot record donation");
            if (listener != null) {
                listener.onError("User not authenticated");
            }
        }
    }
    
    /**
     * Get user's donation history
     */
    public void loadDonationHistory(OnDonationHistoryListener listener) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            firebaseManager.getDonorDonations(currentUser.getUid(), 
                    new FirebaseManager.OnDataReceiveListener<DataSnapshot>() {
                        @Override
                        public void onSuccess(DataSnapshot snapshot) {
                            listener.onHistoryLoaded(snapshot);
                        }
                        
                        @Override
                        public void onError(String error) {
                            showToast("Error loading donation history: " + error);
                            listener.onError(error);
                        }
                    });
        }
    }
    
    // ==================== BLOOD REQUESTS ====================
    
    /**
     * Create emergency blood request
     */
    public void createEmergencyRequest(String bloodType, int units, String hospital, 
                                       String location, String description) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            BloodRequest request = new BloodRequest(bloodType, units, hospital, location, description, "EMERGENCY");
            request.setCreatedBy(currentUser.getUid());
            
            firebaseManager.createBloodRequest(request, 
                    new FirebaseManager.OnCompleteListener() {
                        @Override
                        public void onSuccess(String message) {
                            showToast("🚨 Emergency request created! " + message);
                        }
                        
                        @Override
                        public void onError(String error) {
                            showToast("Error: " + error);
                        }
                    });
        }
    }
    
    /**
     * Get all active blood requests
     */
    public void loadActiveRequests(OnRequestsLoadedListener listener) {
        firebaseManager.getActiveBloodRequests(
                new FirebaseManager.OnDataReceiveListener<DataSnapshot>() {
                    @Override
                    public void onSuccess(DataSnapshot snapshot) {
                        listener.onRequestsLoaded(snapshot);
                    }
                    
                    @Override
                    public void onError(String error) {
                        showToast("Error loading requests: " + error);
                        listener.onError(error);
                    }
                });
    }
    
    /**
     * Get blood requests for specific blood type
     */
    public void getRequestsByBloodType(String bloodType, OnRequestsLoadedListener listener) {
        firebaseManager.getBloodRequestsByType(bloodType, 
                new FirebaseManager.OnDataReceiveListener<DataSnapshot>() {
                    @Override
                    public void onSuccess(DataSnapshot snapshot) {
                        listener.onRequestsLoaded(snapshot);
                    }
                    
                    @Override
                    public void onError(String error) {
                        showToast("Error: " + error);
                        listener.onError(error);
                    }
                });
    }
    
    // ==================== UTILITY METHODS ====================
    
    private void showToast(String message) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
    }
    
    public int getStatusColor(String status) {
        if (status == null) {
            return Color.BLACK;
        }
        switch (status.toUpperCase()) {
            case "AVAILABLE":
                return Color.parseColor("#4CAF50"); // Green
            case "LOW":
                return Color.parseColor("#FF9800"); // Orange
            case "CRITICAL":
                return Color.parseColor("#F44336"); // Red
            default:
                return Color.BLACK;
        }
    }
    
    // ==================== LISTENER INTERFACES ====================
    
    public interface OnCompleteListener {
        void onSuccess(String message);
        void onError(String error);
    }
    
    public interface OnProfileLoadedListener {
        void onProfileLoaded(UserProfile profile);
        void onError(String error);
    }
    
    public interface OnInventoryLoadedListener {
        void onInventoryLoaded(DataSnapshot snapshot);
        void onError(String error);
    }
    
    public interface OnBloodInventoryListener {
        void onSuccess(com.example.blood.models.BloodInventory inventory);
        void onError(String error);
    }
    
    public interface OnDonationHistoryListener {
        void onHistoryLoaded(DataSnapshot snapshot);
        void onError(String error);
    }
    
    public interface OnRequestsLoadedListener {
        void onRequestsLoaded(DataSnapshot snapshot);
        void onError(String error);
    }
}
