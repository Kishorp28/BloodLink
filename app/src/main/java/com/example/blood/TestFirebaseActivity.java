package com.example.blood;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

/**
 * Test Firebase connection and rules
 * Use this to verify Firebase is properly configured
 */
public class TestFirebaseActivity extends AppCompatActivity {
    
    private static final String TAG = "TestFirebaseActivity";
    private TextView statusView;
    private Button testAuthBtn, testWriteBtn, testReadBtn, loadDataBtn;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_firebase);
        
        statusView = findViewById(R.id.statusView);
        testAuthBtn = findViewById(R.id.testAuthBtn);
        testWriteBtn = findViewById(R.id.testWriteBtn);
        testReadBtn = findViewById(R.id.testReadBtn);
        loadDataBtn = findViewById(R.id.loadDataBtn);
        
        firebaseAuth = FirebaseAuth.getInstance();
        
        // Test Authentication Status
        testAuthBtn.setOnClickListener(v -> {
            StringBuilder status = new StringBuilder("=== FIREBASE AUTH TEST ===\n\n");
            
            if (firebaseAuth.getCurrentUser() == null) {
                status.append("❌ NOT LOGGED IN\n");
                status.append("You must login before donating\n\n");
                Log.e(TAG, "User not authenticated");
            } else {
                String userId = firebaseAuth.getCurrentUser().getUid();
                String email = firebaseAuth.getCurrentUser().getEmail();
                status.append("✅ LOGGED IN\n");
                status.append("User ID: ").append(userId).append("\n");
                status.append("Email: ").append(email).append("\n\n");
                Log.d(TAG, "User authenticated: " + userId);
            }
            
            statusView.setText(status.toString());
        });
        
        // Test Database Write (requires rules to be deployed)
        testWriteBtn.setOnClickListener(v -> {
            statusView.setText("Testing database write...\n\n");
            
            if (firebaseAuth.getCurrentUser() == null) {
                statusView.append("❌ Not authenticated - login first");
                return;
            }
            
            String testData = "test_" + System.currentTimeMillis();
            
            FirebaseDatabase.getInstance()
                    .getReference("test_writes")
                    .child(firebaseAuth.getCurrentUser().getUid())
                    .setValue(testData, (error, ref) -> {
                        if (error == null) {
                            statusView.append("✅ WRITE SUCCESSFUL\n");
                            statusView.append("Data written to: /test_writes/" + firebaseAuth.getCurrentUser().getUid() + "\n\n");
                            statusView.append("This means:\n");
                            statusView.append("• Firebase rules ARE deployed\n");
                            statusView.append("• You can write donations\n\n");
                            statusView.append("Value: " + testData);
                            Log.d(TAG, "Write successful");
                        } else {
                            statusView.append("❌ WRITE FAILED\n");
                            statusView.append("Error: " + error.getMessage() + "\n\n");
                            statusView.append("This means:\n");
                            statusView.append("• Firebase rules NOT deployed to Console\n");
                            statusView.append("• OR rules are incorrect\n\n");
                            statusView.append("FIX: Go to Firebase Console → Realtime Database → Rules\n");
                            statusView.append("Copy firebase_rules.json and Publish");
                            Log.e(TAG, "Write failed: " + error.getMessage());
                        }
                    });
        });
        
        // Test Database Read
        testReadBtn.setOnClickListener(v -> {
            statusView.setText("Reading from Firebase...\n\n");
            
            FirebaseDatabase.getInstance()
                    .getReference("inventory")
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot.exists()) {
                            statusView.append("✅ READ SUCCESSFUL\n");
                            statusView.append("Found " + snapshot.getChildrenCount() + " blood types\n\n");
                            
                            for (var child : snapshot.getChildren()) {
                                String bloodType = child.getKey();
                                Integer units = child.child("units").getValue(Integer.class);
                                statusView.append("• " + bloodType + ": " + units + " units\n");
                            }
                            Log.d(TAG, "Read successful, found " + snapshot.getChildrenCount() + " items");
                        } else {
                            statusView.append("❌ NO DATA IN FIREBASE\n\n");
                            statusView.append("The /inventory/ path is empty\n\n");
                            statusView.append("This means:\n");
                            statusView.append("• AdminSetupActivity hasn't been run\n");
                            statusView.append("• OR initial data wasn't loaded\n\n");
                            statusView.append("FIX: Click 'Load Sample Data' button below");
                            Log.w(TAG, "No inventory data found");
                        }
                    })
                    .addOnFailureListener(e -> {
                        statusView.append("❌ READ FAILED\n");
                        statusView.append("Error: " + e.getMessage() + "\n\n");
                        statusView.append("This means:\n");
                        statusView.append("• Firebase not properly initialized\n");
                        statusView.append("• OR google-services.json is invalid\n");
                        Log.e(TAG, "Read failed: " + e.getMessage());
                    });
        });
        
        // Load Sample Data
        loadDataBtn.setOnClickListener(v -> {
            if (firebaseAuth.getCurrentUser() == null) {
                statusView.setText("❌ Login first before loading data\n\n");
                statusView.append("You must be authenticated to load data");
                return;
            }
            
            statusView.setText("Loading sample data into Firebase...\n\n");
            loadDataBtn.setEnabled(false);
            
            com.example.blood.firebase.InitialDataLoader.initializeInventory();
            
            statusView.append("✅ Sample data loaded!\n\n");
            statusView.append("Loaded 8 blood types:\n");
            statusView.append("• A+, A-, B+, B-, O+, O-, AB+, AB-\n\n");
            statusView.append("Check Firebase Console to verify");
            
            Toast.makeText(this, "✓ Data loaded! Refresh the app", Toast.LENGTH_LONG).show();
            Log.d(TAG, "Sample data initialized");
            
            loadDataBtn.setEnabled(true);
        });
    }
}
