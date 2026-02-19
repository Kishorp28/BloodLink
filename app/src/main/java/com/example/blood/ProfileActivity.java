package com.example.blood;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.blood.utils.FirebaseHelper;
import com.example.blood.models.UserProfile;
import com.example.blood.models.BloodDonation;
import com.google.firebase.database.DataSnapshot;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import android.content.Intent;

import java.util.Date;

public class ProfileActivity extends AppCompatActivity {

    private FirebaseHelper firebaseHelper;
    private LinearLayout donationHistoryContainer;
    private TextView tvUserName, tvUserBloodType, tvTotalDonations;
    private String userId;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile); // You'll need to create this layout

        // Initialize Firebase Helper
        firebaseHelper = new FirebaseHelper(this);

        // Get userId from intent
        userId = getIntent().getStringExtra("userId");

        // Initialize views
        tvUserName = findViewById(R.id.userName);
        tvUserBloodType = findViewById(R.id.userBloodType);
        tvTotalDonations = findViewById(R.id.totalDonations);
        donationHistoryContainer = findViewById(R.id.donationHistoryContainer);

        // Setup Drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        setupDrawerNavigation();

        // Get userId from intent
        userId = getIntent().getStringExtra("userId");

        // If no userId from intent, try to get from FirebaseAuth
        if (userId == null || userId.isEmpty()) {
            com.google.firebase.auth.FirebaseAuth auth = com.google.firebase.auth.FirebaseAuth.getInstance();
            if (auth.getCurrentUser() != null) {
                userId = auth.getCurrentUser().getUid();
            }
        }

        // Load user profile and donation history
        if (userId != null && !userId.isEmpty()) {
            loadUserProfile();
            loadDonationHistory();
        } else {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh profile data when user returns
        if (userId != null && !userId.isEmpty()) {
            loadUserProfile();
            loadDonationHistory();
        }
    }

    private void loadUserProfile() {
        firebaseHelper.loadUserProfile(new FirebaseHelper.OnProfileLoadedListener() {
            @Override
            public void onProfileLoaded(UserProfile profile) {
                if (profile != null) {
                    // Update UI with profile safely
                    tvUserName.setText(profile.getName() != null ? profile.getName() : "Unknown");
                    tvUserBloodType.setText("Blood Type: " + (profile.getBloodType() != null ? profile.getBloodType() : "Not specified"));
                    tvTotalDonations.setText("Total Donations: " + profile.getTotalDonations());
                } else {
                    tvUserName.setText("Profile not found");
                    tvUserBloodType.setText("Blood Type: -");
                    tvTotalDonations.setText("Total Donations: 0");
                    Toast.makeText(ProfileActivity.this, "Profile not found - check if you completed signup", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(ProfileActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadDonationHistory() {
        firebaseHelper.loadDonationHistory(new FirebaseHelper.OnDonationHistoryListener() {
            @Override
            public void onHistoryLoaded(DataSnapshot snapshot) {
                donationHistoryContainer.removeAllViews();

                if (!snapshot.exists()) {
                    TextView noDataText = new TextView(ProfileActivity.this);
                    noDataText.setText("No donation history yet");
                    noDataText.setTextSize(16);
                    noDataText.setPadding(20, 20, 20, 20);
                    donationHistoryContainer.addView(noDataText);
                    return;
                }

                for (DataSnapshot donationSnapshot : snapshot.getChildren()) {
                    BloodDonation donation = donationSnapshot.getValue(BloodDonation.class);

                    if (donation != null) {
                        // Create UI for each donation
                        LinearLayout donationItem = new LinearLayout(ProfileActivity.this);
                        donationItem.setOrientation(LinearLayout.VERTICAL);
                        donationItem.setBackgroundColor(android.graphics.Color.parseColor("#F5F5F5"));
                        donationItem.setPadding(16, 16, 16, 16);

                        LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        );
                        itemParams.setMargins(0, 8, 0, 8);
                        donationItem.setLayoutParams(itemParams);

                        // Date
                        TextView tvDate = new TextView(ProfileActivity.this);
                        tvDate.setText("Date: " + new Date(donation.getDonationDate()).toString());
                        tvDate.setTextSize(14);
                        tvDate.setTextColor(android.graphics.Color.BLACK);
                        tvDate.setPadding(0, 0, 0, 8);

                        // Blood Type
                        TextView tvBlood = new TextView(ProfileActivity.this);
                        tvBlood.setText("Blood Type: " + donation.getBloodType());
                        tvBlood.setTextSize(14);
                        tvBlood.setTextColor(android.graphics.Color.BLACK);
                        tvBlood.setPadding(0, 0, 0, 8);

                        // Units
                        TextView tvUnits = new TextView(ProfileActivity.this);
                        tvUnits.setText("Units: " + donation.getUnits());
                        tvUnits.setTextSize(14);
                        tvUnits.setTextColor(android.graphics.Color.GRAY);
                        tvUnits.setPadding(0, 0, 0, 8);

                        // Center
                        TextView tvCenter = new TextView(ProfileActivity.this);
                        tvCenter.setText("Center: " + donation.getDonationCenter());
                        tvCenter.setTextSize(14);
                        tvCenter.setTextColor(android.graphics.Color.GRAY);
                        tvCenter.setPadding(0, 0, 0, 8);

                        // Status
                        TextView tvStatus = new TextView(ProfileActivity.this);
                        tvStatus.setText("Status: " + donation.getStatus());
                        tvStatus.setTextSize(14);
                        tvStatus.setTextColor(android.graphics.Color.parseColor("#D32F2F"));

                        donationItem.addView(tvDate);
                        donationItem.addView(tvBlood);
                        donationItem.addView(tvUnits);
                        donationItem.addView(tvCenter);
                        donationItem.addView(tvStatus);
                        donationHistoryContainer.addView(donationItem);
                    }
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(ProfileActivity.this, "Error loading history: " + error, Toast.LENGTH_SHORT).show();
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
                startActivity(new Intent(this, DonationHistory.class));
            } else if (id == R.id.nav_profile) {
                return true;
            } else if (id == R.id.nav_analytics) {
                startActivity(new Intent(this, AnalyticsActivity.class));
            } else if (id == R.id.nav_emergency) {
                startActivity(new Intent(this, EmergencyRequestActivity.class));
            }
            return true;
        });
    }
}
