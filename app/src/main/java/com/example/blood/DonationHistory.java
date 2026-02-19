package com.example.blood;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class DonationHistory extends AppCompatActivity {

    LinearLayout donationListContainer, compatibleRequestsContainer;
    TextView tvDonationCount, tvTotalVolume, tvHeroLevel, tvLivesSaved, tvEligibilityStatus, tvRequestsHeader;
    BottomNavigationView bottomNav;
    DatabaseReference databaseReference;
    String currentUserId;
    String userBloodType;
    DrawerLayout drawerLayout;
    NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.donation_history);

        donationListContainer = findViewById(R.id.donationListContainer);
        compatibleRequestsContainer = findViewById(R.id.compatibleRequestsContainer);
        tvDonationCount = findViewById(R.id.tvDonationCount);
        tvTotalVolume = findViewById(R.id.tvTotalVolume);
        tvHeroLevel = findViewById(R.id.tvHeroLevel);
        tvLivesSaved = findViewById(R.id.tvLivesSaved);
        tvEligibilityStatus = findViewById(R.id.tvEligibilityStatus);
        tvRequestsHeader = findViewById(R.id.tvRequestsHeader);
        bottomNav = findViewById(R.id.bottomNav);

        databaseReference = FirebaseDatabase.getInstance().getReference();

        // Setup Drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        setupDrawerNavigation();
        // Seed database with sample compatible requests for verification
        
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            currentUserId = user.getUid();
            fetchUserAndLoadData();
        } else {
            finish();
        }

        setupBottomNavigation();
    }

    private void fetchUserAndLoadData() {
        databaseReference.child("users").child(currentUserId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    userBloodType = snapshot.child("bloodType").getValue(String.class);
                    loadDonationHistory(currentUserId);
                    loadCompatibleRequests();
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {}
            });
    }

    private void loadDonationHistory(String userId) {

        // Use correct Firebase path: "blood_donations" not "Donations"
        databaseReference.child("blood_donations").orderByChild("donorId").equalTo(userId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        donationListContainer.removeAllViews();
                        int count = 0;
                        int totalVolume = 0;
                        java.util.Date lastDonationDate = null;
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());

                        if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                            updateStats(0, 0, null);
                            return;
                        }

                        for (DataSnapshot donationSnap : snapshot.getChildren()) {
                            String dateStr = null;
                            Object dateObj = donationSnap.child("date").getValue();
                            if (dateObj == null) dateObj = donationSnap.child("donationDate").getValue();
                            
                            if (dateObj instanceof Long) {
                                dateStr = sdf.format(new java.util.Date((Long) dateObj));
                            } else if (dateObj != null) {
                                dateStr = dateObj.toString();
                            }
                            
                            String bloodGroup = donationSnap.child("bloodGroup").getValue(String.class);
                            if (bloodGroup == null) bloodGroup = donationSnap.child("bloodType").getValue(String.class);
                            
                            String volumeStr = donationSnap.child("volume").getValue(String.class);
                            if (volumeStr == null) {
                                Object unitsObj = donationSnap.child("units").getValue();
                                if (unitsObj instanceof Long || unitsObj instanceof Integer) {
                                    volumeStr = unitsObj.toString() + " ml";
                                } else {
                                    volumeStr = "0 ml";
                                }
                            }
                            
                            String status = donationSnap.child("status").getValue(String.class);
                            if (status == null) status = "Completed";

                            try {
                                if (dateStr != null) {
                                    java.util.Date d = sdf.parse(dateStr);
                                    if (lastDonationDate == null || d.after(lastDonationDate)) {
                                        lastDonationDate = d;
                                    }
                                }
                            } catch (Exception e) {}

                            addDonationCard(dateStr, bloodGroup, volumeStr, status);

                            count++;
                            try {
                                totalVolume += Integer.parseInt(volumeStr.replaceAll("[^0-9]", ""));
                            } catch (Exception e) {}
                        }

                        updateStats(count, totalVolume, lastDonationDate);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
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
                return true;
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

    private void setupBottomNavigation() {
        bottomNav.setSelectedItemId(R.id.nav_history);
        
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.nav_home) {
                Intent intent = new Intent(DonationHistory.this, Dashboard.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_request) {
                Intent intent = new Intent(DonationHistory.this, RequestBlood.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_inventory) {
                Intent intent = new Intent(DonationHistory.this, BloodInventoryActivity.class);
                String userId = getIntent().getStringExtra("userId");
                if (userId != null) {
                    intent.putExtra("userId", userId);
                }
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_history) {
                return true;
            } else if (itemId == R.id.nav_profile) {
                Intent profileIntent = new Intent(DonationHistory.this, Profile.class);
                profileIntent.putExtra("userId", getIntent().getStringExtra("userId"));
                startActivity(profileIntent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }

    private void loadCompatibleRequests() {
        if (userBloodType == null) return;

        databaseReference.child("blood_requests")
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    compatibleRequestsContainer.removeAllViews();
                    boolean hasRequests = false;

                    for (DataSnapshot requestSnap : snapshot.getChildren()) {
                        com.example.blood.models.BloodRequest request = requestSnap.getValue(com.example.blood.models.BloodRequest.class);
                        if (request != null && "PENDING".equals(request.getStatus())) {
                            if (isCompatible(userBloodType, request.getBloodType())) {
                                addRequestCard(request);
                                hasRequests = true;
                            }
                        }
                    }

                    if (hasRequests) {
                        tvRequestsHeader.setVisibility(android.view.View.VISIBLE);
                        tvRequestsHeader.setText("Requests for You (" + userBloodType + ")");
                    } else {
                        tvRequestsHeader.setVisibility(android.view.View.VISIBLE);
                        tvRequestsHeader.setText("No matching requests for " + userBloodType);
                        
                        TextView emptyText = new TextView(DonationHistory.this);
                        emptyText.setText("We'll notify you when someone needs " + userBloodType + " blood.");
                        emptyText.setPadding(20, 20, 20, 20);
                        emptyText.setTextColor(android.graphics.Color.GRAY);
                        compatibleRequestsContainer.addView(emptyText);
                    }
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {}
            });
    }

    private boolean isCompatible(String donor, String recipient) {
        if (donor == null || recipient == null) return false;
        if (donor.equals(recipient)) return true;
        if (donor.equals("O-")) return true; // Universal donor
        if (recipient.equals("AB+")) return true; // Universal recipient

        switch (donor) {
            case "O+": return recipient.equals("O+") || recipient.equals("A+") || recipient.equals("B+") || recipient.equals("AB+");
            case "A+": return recipient.equals("A+") || recipient.equals("AB+");
            case "A-": return recipient.equals("A-") || recipient.equals("A+") || recipient.equals("AB-") || recipient.equals("AB+");
            case "B+": return recipient.equals("B+") || recipient.equals("AB+");
            case "B-": return recipient.equals("B-") || recipient.equals("B+") || recipient.equals("AB-") || recipient.equals("AB+");
            case "AB-": return recipient.equals("AB-") || recipient.equals("AB+");
            default: return donor.equals(recipient);
        }
    }

    private void addRequestCard(com.example.blood.models.BloodRequest request) {
        com.google.android.material.card.MaterialCardView card = new com.google.android.material.card.MaterialCardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 16);
        card.setLayoutParams(params);
        card.setRadius(16);
        card.setCardElevation(4);
        card.setStrokeWidth(2);
        card.setStrokeColor(android.graphics.Color.parseColor("#FFEBEE"));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);

        TextView title = new TextView(this);
        title.setText("Urgent: " + request.getBloodType() + " Needed");
        title.setTextColor(android.graphics.Color.RED);
        title.setTextSize(16);
        title.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView hospital = new TextView(this);
        hospital.setText(request.getHospitalName() + " (" + request.getUnitsNeeded() + " Units)");
        hospital.setTextSize(14);
        hospital.setPadding(0, 4, 0, 8);

        LinearLayout btnLayout = new LinearLayout(this);
        btnLayout.setOrientation(LinearLayout.HORIZONTAL);
        btnLayout.setGravity(android.view.Gravity.END);

        com.google.android.material.button.MaterialButton btnAccept = new com.google.android.material.button.MaterialButton(this, null, com.google.android.material.R.attr.materialButtonStyle);
        btnAccept.setText("Accept");
        btnAccept.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#4CAF50")));
        btnAccept.setOnClickListener(v -> showAcceptDialog(request));

        com.google.android.material.button.MaterialButton btnReject = new com.google.android.material.button.MaterialButton(this, null, com.google.android.material.R.attr.borderlessButtonStyle);
        btnReject.setText("No thanks");
        btnReject.setOnClickListener(v -> card.setVisibility(android.view.View.GONE));

        btnLayout.addView(btnReject);
        btnLayout.addView(btnAccept);

        layout.addView(title);
        layout.addView(hospital);
        layout.addView(btnLayout);
        card.addView(layout);
        compatibleRequestsContainer.addView(card);
    }

    private void showAcceptDialog(com.example.blood.models.BloodRequest request) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        new android.app.DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String date = dayOfMonth + "/" + (month + 1) + "/" + year;
            saveDonation(request, date);
        }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH)).show();
    }


    private void saveDonation(com.example.blood.models.BloodRequest request, String date) {
        String donationId = databaseReference.child("blood_donations").push().getKey();
        com.example.blood.models.BloodDonation donation = new com.example.blood.models.BloodDonation(
            currentUserId, userBloodType, request.getUnitsNeeded(), request.getHospitalName());
        donation.setDonationId(donationId);
        donation.setStatus("COMPLETED");
        
        // Manual date override for history
        databaseReference.child("blood_donations").child(donationId).setValue(donation)
            .addOnSuccessListener(aVoid -> {
                // Update history entry with text date (since model uses long but UI handles both)
                databaseReference.child("blood_donations").child(donationId).child("date").setValue(date);
                android.widget.Toast.makeText(this, "Donation added successfully!", android.widget.Toast.LENGTH_SHORT).show();
            });
    }

    private void updateStats(int count, int totalVolume, java.util.Date lastDate) {
        tvDonationCount.setText(String.valueOf(count));
        tvTotalVolume.setText(totalVolume + " ml");
        tvLivesSaved.setText((count * 3) + " lives saved so far");

        // Hero Level
        String level = "Rookie Hero";
        if (count >= 10) level = "Elite Guardian";
        else if (count >= 5) level = "Veteran Lifesaver";
        else if (count >= 2) level = "Silver Donor";
        tvHeroLevel.setText(level);

        // Eligibility
        if (lastDate == null) {
            tvEligibilityStatus.setText("You are eligible to donate now!");
        } else {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(lastDate);
            cal.add(java.util.Calendar.DAY_OF_YEAR, 90);
            java.util.Date nextEligible = cal.getTime();
            java.util.Date today = new java.util.Date();

            if (today.after(nextEligible)) {
                tvEligibilityStatus.setText("You are eligible to donate now!");
            } else {
                long diff = nextEligible.getTime() - today.getTime();
                long days = diff / (24 * 60 * 60 * 1000);
                tvEligibilityStatus.setText("Eligible in " + days + " days (" + 
                        new java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault()).format(nextEligible) + ")");
            }
        }
    }

    private void addDonationCard(String date, String group, String volume, String status) {
        com.google.android.material.card.MaterialCardView card = 
            new com.google.android.material.card.MaterialCardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        card.setLayoutParams(params);
        card.setCardBackgroundColor(android.graphics.Color.WHITE);
        card.setRadius(16);
        card.setCardElevation(2);
        
        android.view.View view = getLayoutInflater().inflate(R.layout.notification_item, null);
        ((TextView)view.findViewById(R.id.tvNotifTitle)).setText("Donation at " + (group != null ? group : "Blood Bank"));
        ((TextView)view.findViewById(R.id.tvNotifBody)).setText(volume + " • " + status);
        ((TextView)view.findViewById(R.id.tvNotifTime)).setText(date);
        
        card.addView(view);
        donationListContainer.addView(card, 0); // Recent at top
    }

    @Override
    public void onBackPressed() {
        // Navigate back to Dashboard
        Intent intent = new Intent(DonationHistory.this, Dashboard.class);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }
}
