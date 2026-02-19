package com.example.blood.integration;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.blood.R;
import com.example.blood.models.BloodInventory;
import com.example.blood.models.UserProfile;
import com.example.blood.utils.FirebaseHelper;
import com.google.firebase.database.DataSnapshot;

/**
 * EXAMPLE: How to integrate Firebase in your Dashboard Activity
 */
public class DashboardIntegrationExample extends AppCompatActivity {

    private FirebaseHelper firebaseHelper;
    private LinearLayout bloodTypeContainer, donorContainer;
    private TextView userNameText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dashboard);

        // Initialize Firebase Helper
        firebaseHelper = new FirebaseHelper(this);

        // Initialize views
        bloodTypeContainer = findViewById(R.id.bloodContainer);
        donorContainer = findViewById(R.id.donorContainer);
        userNameText = findViewById(R.id.user);

        // Load data
        loadUserProfile();
        loadBloodInventory();
        loadNearbyDonors();
    }

    /**
     * Load current user's profile and display name
     */
    private void loadUserProfile() {
        firebaseHelper.loadUserProfile(new FirebaseHelper.OnProfileLoadedListener() {
            @Override
            public void onProfileLoaded(UserProfile profile) {
                userNameText.setText("Hello, " + profile.getName());
            }

            @Override
            public void onError(String error) {
                Toast.makeText(DashboardIntegrationExample.this,
                        "Error loading profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Load blood inventory and display in grid
     */
    private void loadBloodInventory() {
        firebaseHelper.loadBloodInventory(new FirebaseHelper.OnInventoryLoadedListener() {
            @Override
            public void onInventoryLoaded(DataSnapshot snapshot) {
                bloodTypeContainer.removeAllViews();

                for (DataSnapshot bloodSnapshot : snapshot.getChildren()) {
                    BloodInventory inventory = bloodSnapshot.getValue(BloodInventory.class);
                    if (inventory != null) {
                        // Create blood type item (you can use custom layout)
                        LinearLayout bloodItem = createBloodTypeItem(inventory);
                        bloodTypeContainer.addView(bloodItem);
                    }
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(DashboardIntegrationExample.this,
                        "Error loading inventory", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Create individual blood type display item
     */
    private LinearLayout createBloodTypeItem(BloodInventory inventory) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setPadding(16, 16, 16, 16);

        // Blood type text
        TextView bloodType = new TextView(this);
        bloodType.setText(inventory.getBloodType());
        bloodType.setTextSize(24);
        bloodType.setTypeface(null, android.graphics.Typeface.BOLD);
        item.addView(bloodType);

        // Units text
        TextView units = new TextView(this);
        units.setText(inventory.getUnits() + " units");
        units.setTextSize(14);
        item.addView(units);

        // Status text with color
        TextView status = new TextView(this);
        status.setText(inventory.getStatus());
        status.setTextSize(12);

        int statusColor = getStatusColor(inventory.getStatus());
        status.setTextColor(statusColor);
        item.addView(status);

        return item;
    }

    /**
     * Get color based on status
     */
    private int getStatusColor(String status) {
        switch (status) {
            case "AVAILABLE":
                return getColor(R.color.status_available);
            case "LOW":
                return getColor(R.color.status_low);
            case "CRITICAL":
                return getColor(R.color.status_critical);
            default:
                return getColor(R.color.gray_700);
        }
    }

    /**
     * Load nearby donors (those with similar blood type in same city)
     */
    private void loadNearbyDonors() {
        firebaseHelper.loadUserProfile(new FirebaseHelper.OnProfileLoadedListener() {
            @Override
            public void onProfileLoaded(UserProfile userProfile) {
                // Query donors with same blood type in same city
                // You'll need to implement custom query in FirebaseManager
                // For now, show placeholder
                TextView placeholder = new TextView(DashboardIntegrationExample.this);
                placeholder.setText("Nearby donors loading...");
                donorContainer.addView(placeholder);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(DashboardIntegrationExample.this,
                        "Error loading nearby donors", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
