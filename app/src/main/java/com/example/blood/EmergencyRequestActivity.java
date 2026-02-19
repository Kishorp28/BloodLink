package com.example.blood;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.FrameLayout;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.app.PendingIntent;
import android.app.Activity;
import android.Manifest;
import android.content.pm.PackageManager;
import android.telephony.SmsManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

import com.example.blood.utils.FirebaseHelper;
import androidx.annotation.NonNull;

public class EmergencyRequestActivity extends AppCompatActivity {

    private FirebaseHelper firebaseHelper;
    private AutoCompleteTextView bloodTypeSpinner;
    private EditText unitsInput, hospitalNameInput, locationInput, descriptionInput;
    private Button submitBtn;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private FrameLayout loadingOverlay;
    private static final int SMS_PERMISSION_CODE = 101;
    private static final String SMS_SENT = "SMS_SENT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_request); // You'll need to create this layout

        // Initialize Firebase Helper
        firebaseHelper = new FirebaseHelper(this);

        // Initialize views
        bloodTypeSpinner = findViewById(R.id.bloodTypeSpinner);
        unitsInput = findViewById(R.id.unitsInput);
        hospitalNameInput = findViewById(R.id.hospitalNameInput);
        locationInput = findViewById(R.id.locationInput);
        descriptionInput = findViewById(R.id.descriptionInput);
        submitBtn = findViewById(R.id.submitBtn);
        loadingOverlay = findViewById(R.id.loadingOverlay);

        // Setup Drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        setupDrawerNavigation();

        // Setup blood type dropdown
        String[] bloodTypes = {"A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-", "Any"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                bloodTypes
        );
        bloodTypeSpinner.setAdapter(adapter);
        bloodTypeSpinner.setInputType(android.text.InputType.TYPE_NULL);
        bloodTypeSpinner.setOnClickListener(v -> bloodTypeSpinner.showDropDown());
        bloodTypeSpinner.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) bloodTypeSpinner.showDropDown();
        });

        // Submit button click listener
        submitBtn.setOnClickListener(v -> createEmergencyRequest());
    }

    private void createEmergencyRequest() {
        String bloodType = bloodTypeSpinner.getText().toString().trim();
        String unitsStr = unitsInput.getText().toString().trim();
        String hospitalName = hospitalNameInput.getText().toString().trim();
        String location = locationInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();

        // Validation
        if (bloodType.isEmpty()) {
            Toast.makeText(this, "Please select blood type", Toast.LENGTH_SHORT).show();
            return;
        }

        if (unitsStr.isEmpty()) {
            Toast.makeText(this, "Please enter units needed", Toast.LENGTH_SHORT).show();
            return;
        }

        if (hospitalName.isEmpty()) {
            Toast.makeText(this, "Please enter hospital name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (location.isEmpty()) {
            Toast.makeText(this, "Please enter location", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            submitRequestAndBroadcast();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_CODE);
        }
    }

    private void submitRequestAndBroadcast() {
        String bloodType = bloodTypeSpinner.getText().toString().trim();
        String unitsStr = unitsInput.getText().toString().trim();
        String hospitalName = hospitalNameInput.getText().toString().trim();
        String location = locationInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();

        // Create emergency request
        firebaseHelper.createEmergencyRequest(
                bloodType,
                Integer.parseInt(unitsStr),
                hospitalName,
                location,
                description.isEmpty() ? "Emergency blood request" : description
        );

        // Clear fields
        bloodTypeSpinner.setText("");
        unitsInput.setText("");
        hospitalNameInput.setText("");
        locationInput.setText("");
        descriptionInput.setText("");

        Toast.makeText(this, "Emergency request submitted!", Toast.LENGTH_LONG).show();
        
        // Show loading overlay
        loadingOverlay.setVisibility(android.view.View.VISIBLE);
        
        // Broadcast SMS to all donors
        broadcastEmergencySMS(bloodType, hospitalName, location);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                submitRequestAndBroadcast();
            } else {
                Toast.makeText(this, "SMS Permission denied. Request saved to database only.", Toast.LENGTH_LONG).show();
                // Submitting without broadcast if denied
                String bloodType = bloodTypeSpinner.getText().toString().trim();
                String unitsStr = unitsInput.getText().toString().trim();
                String hospitalName = hospitalNameInput.getText().toString().trim();
                String location = locationInput.getText().toString().trim();
                String description = descriptionInput.getText().toString().trim();
                
                firebaseHelper.createEmergencyRequest(
                        bloodType,
                        Integer.parseInt(unitsStr),
                        hospitalName,
                        location,
                        description.isEmpty() ? "Emergency blood request" : description
                );
                finish();
            }
        }
    }

    private void broadcastEmergencySMS(String bloodType, String hospital, String location) {
        String message = "EMERGENCY: " + bloodType + " blood needed at " + hospital + " (" + location + "). Please respond if available!";
        
        // Immediate feedback
        Toast.makeText(this, "Fetching users for broadcast...", Toast.LENGTH_SHORT).show();

        FirebaseDatabase.getInstance().getReference("users")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            int sentCount = 0;
                            for (DataSnapshot userSnap : snapshot.getChildren()) {
                                String phone = userSnap.child("phone").getValue(String.class);
                                if (phone != null && !phone.isEmpty()) {
                                    sendSMS(phone, message);
                                    sentCount++;
                                }
                            }
                            
                            if (sentCount > 0) {
                                Toast.makeText(getApplicationContext(), "Emergency alert broadcast to " + sentCount + " users!", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(getApplicationContext(), "No users found in database to alert.", Toast.LENGTH_SHORT).show();
                            }
                        }
                        
                        loadingOverlay.setVisibility(android.view.View.GONE);
                        
                        // Delayed finish to allow user to see the result
                        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                            if (!isFinishing()) finish();
                        }, 1000);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        loadingOverlay.setVisibility(android.view.View.GONE);
                        Toast.makeText(getApplicationContext(), "Broadcast failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendSMS(String phoneNumber, String message) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            try {
                // For a broadcast to many users, we'll send without per-message tracking 
                // to avoid overwhelming the system with receivers.
                // We trust the default SmsManager to queue these.
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            } catch (Exception e) {
                Log.e("SMS", "Error sending to " + phoneNumber, e);
            }
        }
    }

    private void setupDrawerNavigation() {
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            drawerLayout.closeDrawer(GravityCompat.START);
            
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, Dashboard.class));
            } else if (id == R.id.nav_request) {
                startActivity(new Intent(this, RequestBlood.class));
            } else if (id == R.id.nav_inventory) {
                startActivity(new Intent(this, BloodInventoryActivity.class));
            } else if (id == R.id.nav_history) {
                startActivity(new Intent(this, DonationHistory.class));
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, Profile.class));
            } else if (id == R.id.nav_analytics) {
                startActivity(new Intent(this, AnalyticsActivity.class));
            } else if (id == R.id.nav_emergency) {
                return true;
            }
            return true;
        });
    }
}
