package com.example.blood;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RequestBlood extends AppCompatActivity {

    EditText edtUnits, edtLocation, edtPatientName;
    Button btnA_plus, btnA_minus, btnB_plus, btnB_minus, btnAB_plus, btnAB_minus, btnO_plus, btnO_minus;
    Button btnNormal, btnUrgent, btnImmediate, btnSubmitRequest;
    BottomNavigationView bottomNav;

    String selectedBloodGroup = "";
    String selectedUrgency = "Normal";
    DatabaseReference databaseReference;
    DrawerLayout drawerLayout;
    NavigationView navigationView;

    private void setupDrawerNavigation() {
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            drawerLayout.closeDrawer(GravityCompat.START);
            
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, Dashboard.class));
            } else if (id == R.id.nav_request) {
                return true;
            } else if (id == R.id.nav_inventory) {
                startActivity(new Intent(this, BloodInventoryActivity.class));
            } else if (id == R.id.nav_history) {
                startActivity(new Intent(this, DonationHistory.class));
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, Profile.class));
            } else if (id == R.id.nav_analytics) {
                startActivity(new Intent(this, AnalyticsActivity.class));
            } else if (id == R.id.nav_emergency) {
                startActivity(new Intent(this, EmergencyRequestActivity.class));
            }
            return true;
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.request_blood);

        // Initialize views
        edtUnits = findViewById(R.id.edtUnits);
        edtLocation = findViewById(R.id.edtLocation);
        edtPatientName = findViewById(R.id.edtPatientName);

        btnA_plus = findViewById(R.id.btnA_plus);
        btnA_minus = findViewById(R.id.btnA_minus);
        btnB_plus = findViewById(R.id.btnB_plus);
        btnB_minus = findViewById(R.id.btnB_minus);
        btnAB_plus = findViewById(R.id.btnAB_plus);
        btnAB_minus = findViewById(R.id.btnAB_minus);
        btnO_plus = findViewById(R.id.btnO_plus);
        btnO_minus = findViewById(R.id.btnO_minus);

        btnNormal = findViewById(R.id.btnNormal);
        btnUrgent = findViewById(R.id.btnUrgent);
        btnImmediate = findViewById(R.id.btnImmediate);

        btnSubmitRequest = findViewById(R.id.btnSubmitRequest);
        bottomNav = findViewById(R.id.bottomNav);

        databaseReference = FirebaseDatabase.getInstance().getReference("blood_requests");

        // Setup Drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        setupDrawerNavigation();

        // Set up blood group selection
        setupBloodGroupButtons();

        // Set up urgency buttons
        setupUrgencyButtons();

        // Submit button
        btnSubmitRequest.setOnClickListener(v -> submitRequest());

        // Bottom navigation
        setupBottomNavigation();
    }

    private void setupBloodGroupButtons() {
        Button[] buttons = {btnA_plus, btnA_minus, btnB_plus, btnB_minus, btnAB_plus, btnAB_minus, btnO_plus, btnO_minus};
        String[] groups = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};

        for (int i = 0; i < buttons.length; i++) {
            final int index = i;
            buttons[i].setOnClickListener(v -> {
                selectedBloodGroup = groups[index];
                updateButtonSelection(buttons);
                buttons[index].setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#D32F2F")));
                buttons[index].setTextColor(android.graphics.Color.WHITE);
            });
        }
    }

    private void setupUrgencyButtons() {
        btnNormal.setOnClickListener(v -> {
            selectedUrgency = "Normal";
            updateUrgencySelection();
        });

        btnUrgent.setOnClickListener(v -> {
            selectedUrgency = "Urgent";
            updateUrgencySelection();
        });

        btnImmediate.setOnClickListener(v -> {
            selectedUrgency = "Immediate";
            updateUrgencySelection();
        });
    }

    private void updateUrgencySelection() {
        btnNormal.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                selectedUrgency.equals("Normal") ? android.graphics.Color.parseColor("#D32F2F") : android.graphics.Color.parseColor("#E0E0E0")));
        btnNormal.setTextColor(selectedUrgency.equals("Normal") ? android.graphics.Color.WHITE : android.graphics.Color.parseColor("#424242"));

        btnUrgent.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                selectedUrgency.equals("Urgent") ? android.graphics.Color.parseColor("#D32F2F") : android.graphics.Color.parseColor("#E0E0E0")));
        btnUrgent.setTextColor(selectedUrgency.equals("Urgent") ? android.graphics.Color.WHITE : android.graphics.Color.parseColor("#424242"));

        btnImmediate.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                selectedUrgency.equals("Immediate") ? android.graphics.Color.parseColor("#D32F2F") : android.graphics.Color.parseColor("#E0E0E0")));
        btnImmediate.setTextColor(selectedUrgency.equals("Immediate") ? android.graphics.Color.WHITE : android.graphics.Color.parseColor("#424242"));
    }

    private void updateButtonSelection(Button[] buttons) {
        for (Button btn : buttons) {
            btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#E0E0E0")));
            btn.setTextColor(android.graphics.Color.parseColor("#424242"));
        }
    }

    private void submitRequest() {
        String units = edtUnits.getText().toString().trim();
        String location = edtLocation.getText().toString().trim();
        String patientName = edtPatientName.getText().toString().trim();

        if (selectedBloodGroup.isEmpty() || units.isEmpty() || location.isEmpty() || patientName.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String requestId = databaseReference.push().getKey();
        long currentTime = System.currentTimeMillis();

        Map<String, Object> request = new HashMap<>();
        request.put("requestId", requestId);
        request.put("bloodType", selectedBloodGroup);
        request.put("unitsNeeded", Integer.parseInt(units));
        request.put("hospitalName", patientName); // Using patientName field for hospital/patient info
        request.put("location", location);
        request.put("description", "Blood needed for " + patientName);
        request.put("urgency", selectedUrgency.toUpperCase());
        request.put("status", "PENDING");
        request.put("createdTime", currentTime);
        
        String currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null 
                ? com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid() 
                : "anonymous";
        request.put("createdBy", currentUserId);

        databaseReference.child(requestId).setValue(request).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Blood request created! (CRUD - Create)", Toast.LENGTH_SHORT).show();
            notifyMatchingDonors(selectedBloodGroup, patientName, selectedUrgency);
            finish();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to create request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void notifyMatchingDonors(String bloodGroup, String patientName, String urgency) {
        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        
        // 1. Save to Global Notifications so Everyone sees it in their bell icon page
        Map<String, Object> publicNotif = new HashMap<>();
        publicNotif.put("title", "URGENT: " + bloodGroup + " Needed");
        publicNotif.put("body", patientName + " needs " + bloodGroup + " at " + urgency + " level.");
        publicNotif.put("time", time);
        publicNotif.put("type", "PUBLIC");
        
        FirebaseDatabase.getInstance().getReference("PublicNotifications").push().setValue(publicNotif);

        // 2. Also save to matching donors for personalized alerts (optional but good for tracking)
        FirebaseDatabase.getInstance().getReference("users")
                .orderByChild("bloodType").equalTo(bloodGroup)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot userSnap : snapshot.getChildren()) {
                            String userId = userSnap.getKey();
                            if (userId != null) {
                                FirebaseDatabase.getInstance().getReference("Notifications")
                                        .child(userId).push().setValue(publicNotif);
                            }
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void clearFields() {
        edtUnits.setText("");
        edtLocation.setText("");
        edtPatientName.setText("");
        selectedBloodGroup = "";
        selectedUrgency = "Normal";
    }

    private void setupBottomNavigation() {
        bottomNav.setSelectedItemId(R.id.nav_request);
        
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.nav_home) {
                Intent intent = new Intent(RequestBlood.this, Dashboard.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_request) {
                return true;
            } else if (itemId == R.id.nav_inventory) {
                Intent intent = new Intent(RequestBlood.this, BloodInventoryActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_history) {
                Intent intent = new Intent(RequestBlood.this, DonationHistory.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
                Intent intent = new Intent(RequestBlood.this, Profile.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }

    @Override
    public void onBackPressed() {
        // Navigate back to Dashboard
        Intent intent = new Intent(RequestBlood.this, Dashboard.class);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }
}
