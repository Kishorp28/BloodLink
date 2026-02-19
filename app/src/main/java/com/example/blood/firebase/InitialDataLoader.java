package com.example.blood.firebase;

import com.google.firebase.database.FirebaseDatabase;
import com.example.blood.models.BloodInventory;
import com.example.blood.models.BloodRequest;

/**
 * Initializes Firebase Realtime Database with sample blood inventory data
 * Run this ONCE when app is first installed (in Signup or Dashboard onCreate)
 */
public class InitialDataLoader {

    /**
     * Initialize blood inventory with sample data
     * Call this ONCE during app setup
     */
    public static void initializeInventory() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();

        // Sample data for all blood types
        BloodInventory[] bloodInventories = {
                new BloodInventory("A+", 45, "AVAILABLE"),
                new BloodInventory("A-", 28, "AVAILABLE"),
                new BloodInventory("B+", 52, "AVAILABLE"),
                new BloodInventory("B-", 18, "LOW"),
                new BloodInventory("O+", 78, "AVAILABLE"),
                new BloodInventory("O-", 35, "AVAILABLE"),
                new BloodInventory("AB+", 12, "LOW"),
                new BloodInventory("AB-", 8, "CRITICAL")
        };

        // Set each blood type in database
        for (BloodInventory inventory : bloodInventories) {
            inventory.setLastUpdated(System.currentTimeMillis());
            database.getReference("inventory")
                    .child(inventory.getBloodType())
                    .setValue(inventory);
        }
    }

    /**
     * Add sample blood requests
     */
    public static void initializeSampleRequests() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();

        // Sample emergency request
        BloodRequest request1 = new BloodRequest();
        request1.setRequestId(database.getReference("blood_requests").push().getKey());
        request1.setBloodType("O-");
        request1.setUnitsNeeded(5);
        request1.setHospitalName("City General Hospital");
        request1.setLocation("Downtown Medical Center");
        request1.setDescription("Emergency surgery - patient needs O- blood urgently");
        request1.setUrgency("EMERGENCY");
        request1.setStatus("PENDING");
        request1.setCreatedTime(System.currentTimeMillis());
        request1.setExpiryTime(System.currentTimeMillis() + (24 * 60 * 60 * 1000)); // 24 hours

        database.getReference("blood_requests")
                .child(request1.getRequestId())
                .setValue(request1);

        // Sample routine request
        BloodRequest request2 = new BloodRequest();
        request2.setRequestId(database.getReference("blood_requests").push().getKey());
        request2.setBloodType("A+");
        request2.setUnitsNeeded(3);
        request2.setHospitalName("Central Blood Bank");
        request2.setLocation("Medical Plaza");
        request2.setDescription("Routine blood transfusion for patient recovery");
        request2.setUrgency("ROUTINE");
        request2.setStatus("PENDING");
        request2.setCreatedTime(System.currentTimeMillis());
        request2.setExpiryTime(System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000)); // 7 days

        database.getReference("blood_requests")
                .child(request2.getRequestId())
                .setValue(request2);
    }

    /**
     * Initialize with a single blood type (useful for testing)
     */
    public static void addBloodType(String bloodType, int units) {
        BloodInventory inventory = new BloodInventory(
                bloodType,
                units,
                getStatusFromUnits(units)
        );
        inventory.setLastUpdated(System.currentTimeMillis());

        FirebaseDatabase.getInstance()
                .getReference("inventory")
                .child(bloodType)
                .setValue(inventory);
    }

    /**
     * Update units for existing blood type
     */
    public static void updateBloodUnits(String bloodType, int units) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        String status = getStatusFromUnits(units);

        database.getReference("inventory/" + bloodType + "/units").setValue(units);
        database.getReference("inventory/" + bloodType + "/status").setValue(status);
        database.getReference("inventory/" + bloodType + "/lastUpdated")
                .setValue(System.currentTimeMillis());
    }

    /**
     * Determine status based on unit count
     * Status thresholds:
     * - AVAILABLE: >= 20 units
     * - LOW: 10-19 units
     * - CRITICAL: < 10 units
     */
    private static String getStatusFromUnits(int units) {
        if (units >= 20) {
            return "AVAILABLE";
        } else if (units >= 10) {
            return "LOW";
        } else {
            return "CRITICAL";
        }
    }
}
