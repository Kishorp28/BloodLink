package com.example.blood;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.blood.models.BloodInventory;
import com.example.blood.utils.FirebaseHelper;
import com.example.blood.firebase.FirebaseManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class BloodInventoryActivity extends AppCompatActivity {

    private LinearLayout inventoryContainer;
    private TextView tvTotalUnits;
    private BottomNavigationView bottomNav;
    private FirebaseHelper firebaseHelper;
    private android.widget.EditText etSearch;
    private View btnMap;
    
    private java.util.List<com.example.blood.models.BloodBank> allBloodBanks = new java.util.ArrayList<>();
    private double currentLat = 0, currentLon = 0;
    private String userId;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Use the detailed inventory layout
        setContentView(R.layout.blood_inventory);

        inventoryContainer = findViewById(R.id.inventoryContainer);
        tvTotalUnits = findViewById(R.id.tvTotalUnits);
        bottomNav = findViewById(R.id.bottomNav);
        etSearch = findViewById(R.id.etSearch);
        btnMap = findViewById(R.id.btnMap);

        // Setup Drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        setupDrawerNavigation();

        firebaseHelper = new FirebaseHelper(this);
        userId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null 
                ? com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        setupListeners();
        fetchUserLocation();
        loadInventory();
        setupBottomNavigation();
    }

    private void fetchUserLocation() {
        if (userId == null) return;
        com.google.firebase.database.FirebaseDatabase.getInstance().getReference("users").child(userId)
            .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                    Double lat = snapshot.child("latitude").getValue(Double.class);
                    Double lon = snapshot.child("longitude").getValue(Double.class);
                    if (lat != null && lon != null) {
                        currentLat = lat;
                        currentLon = lon;
                        // Refresh display to show distances
                        filterInventory(etSearch.getText().toString());
                    }
                }
                @Override public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {}
            });
    }

    private void setupListeners() {
        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterInventory(s.toString());
            }
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        btnMap.setOnClickListener(v -> {
            Intent intent = new Intent(this, BloodBanksMapActivity.class);
            startActivity(intent);
        });
    }

    private void filterInventory(String query) {
        if (allBloodBanks.isEmpty()) return;
        
        inventoryContainer.removeAllViews();
        int totalUnits = 0;
        String lowerQuery = query.toLowerCase().trim();

        int matchesCount = 0;
        for (com.example.blood.models.BloodBank bank : allBloodBanks) {
            boolean matches = lowerQuery.isEmpty() || 
                             bank.getName().toLowerCase().contains(lowerQuery) || 
                             bank.getAddress().toLowerCase().contains(lowerQuery);
            
            // Also check blood types in inventory
            if (!matches && !lowerQuery.isEmpty()) {
                for (String bloodType : bank.getInventory().keySet()) {
                    if (bloodType.toLowerCase().contains(lowerQuery)) {
                        matches = true;
                        break;
                    }
                }
            }

            if (matches) {
                matchesCount++;
                View hospitalCard = createHospitalCard(bank);
                inventoryContainer.addView(hospitalCard);
                
                for (Integer units : bank.getInventory().values()) {
                    totalUnits += units;
                }
            }
        }
        
        tvTotalUnits.setText(totalUnits + " Units");
        
        if (matchesCount == 0 && !lowerQuery.isEmpty()) {
            TextView tvNoResults = new TextView(this);
            tvNoResults.setText("No hospitals found matching \"" + query + "\"");
            tvNoResults.setGravity(android.view.Gravity.CENTER);
            tvNoResults.setPadding(0, 50, 0, 50);
            tvNoResults.setTextColor(android.graphics.Color.GRAY);
            inventoryContainer.addView(tvNoResults);
        }
    }

    private View createHospitalCard(com.example.blood.models.BloodBank bank) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View hospitalCard = inflater.inflate(R.layout.inventory_row, inventoryContainer, false);
        TextView tvName = hospitalCard.findViewById(R.id.tvHospitalName);
        TextView tvAddress = hospitalCard.findViewById(R.id.tvHospitalAddress);
        TextView tvDistance = hospitalCard.findViewById(R.id.tvHospitalDistance);
        LinearLayout stockContainer = hospitalCard.findViewById(R.id.stockContainer);

        tvName.setText(bank.getName());
        tvAddress.setText(bank.getAddress());

        if (currentLat != 0 && bank.getLatitude() != 0) {
            double dist = calculateDistance(currentLat, currentLon, bank.getLatitude(), bank.getLongitude());
            tvDistance.setText(String.format(java.util.Locale.US, "%.1f km away", dist));
            tvDistance.setVisibility(View.VISIBLE);
        } else {
            tvDistance.setVisibility(View.GONE);
        }

        hospitalCard.findViewById(R.id.btnNavigate).setOnClickListener(v -> {
            if (bank.getLatitude() != 0) {
                String uri = "google.navigation:q=" + bank.getLatitude() + "," + bank.getLongitude();
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uri));
                mapIntent.setPackage("com.google.android.apps.maps");
                if (mapIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(mapIntent);
                } else {
                    // Fallback to web maps or generic geo intent if gmaps not installed
                    String geoUri = "geo:" + bank.getLatitude() + "," + bank.getLongitude() + "?q=" + bank.getName();
                    startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(geoUri)));
                }
            } else {
                android.widget.Toast.makeText(this, "Coordinates not available for " + bank.getName(), android.widget.Toast.LENGTH_SHORT).show();
            }
        });

        for (java.util.Map.Entry<String, Integer> entry : bank.getInventory().entrySet()) {
            String bloodType = entry.getKey();
            Integer units = entry.getValue();

            View stockItem = inflater.inflate(R.layout.blood_type_stock_item, stockContainer, false);
            TextView tvStockType = stockItem.findViewById(R.id.tvStockBloodType);
            TextView tvStockUnits = stockItem.findViewById(R.id.tvStockUnits);

            tvStockType.setText(bloodType);
            tvStockUnits.setText(units + " U");

            if (units < 5) {
                tvStockUnits.setTextColor(android.graphics.Color.parseColor("#F44336"));
            } else if (units < 10) {
                tvStockUnits.setTextColor(android.graphics.Color.parseColor("#FF9800"));
            }
            stockContainer.addView(stockItem);
        }
        return hospitalCard;
    }

    private void loadInventory() {
        FirebaseManager firebaseManager = new FirebaseManager();
        firebaseManager.getAllBloodBanks(new FirebaseManager.OnDataReceiveListener<com.google.firebase.database.DataSnapshot>() {
            @Override
            public void onSuccess(com.google.firebase.database.DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    // Initialize if empty (one-time setup)
                    firebaseManager.initializeCoimbatoreData();
                    // Reload after a short delay
                    inventoryContainer.postDelayed(() -> loadInventory(), 1000);
                    return;
                }

                inventoryContainer.removeAllViews();
                allBloodBanks.clear();
                int totalUnits = 0;
                boolean needsCoordRepair = false;

                for (com.google.firebase.database.DataSnapshot bankSnap : snapshot.getChildren()) {
                    com.example.blood.models.BloodBank bank = bankSnap.getValue(com.example.blood.models.BloodBank.class);
                    if (bank != null) {
                        // Detect stale data (0 coords)
                        if (bank.getLatitude() == 0) needsCoordRepair = true;
                        
                        allBloodBanks.add(bank);
                        View hospitalCard = createHospitalCard(bank);
                        inventoryContainer.addView(hospitalCard);
                        
                        for (Integer units : bank.getInventory().values()) {
                            totalUnits += units;
                        }
                    }
                }
                
                // If any bank is missing coords, push the corrected Coimbatore data
                if (needsCoordRepair) {
                    android.widget.Toast.makeText(BloodInventoryActivity.this, 
                            "Updating coordinate database... Please wait.", android.widget.Toast.LENGTH_SHORT).show();
                    firebaseManager.initializeCoimbatoreData();
                    // Reload to show only the clean corrected data
                    inventoryContainer.postDelayed(() -> loadInventory(), 2000);
                    return;
                }
                
                // Final UI update respecting search
                filterInventory(etSearch.getText().toString());
            }

            @Override
            public void onError(String error) {
                tvTotalUnits.setText("Error");
            }
        });
    }

    private void setupBottomNavigation() {
        bottomNav.setSelectedItemId(R.id.nav_inventory);

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                Intent intent = new Intent(BloodInventoryActivity.this, Dashboard.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_request) {
                Intent intent = new Intent(BloodInventoryActivity.this, RequestBlood.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_inventory) {
                return true;
            } else if (itemId == R.id.nav_history) {
                Intent historyIntent = new Intent(BloodInventoryActivity.this, DonationHistory.class);
                // Pass userId if available
                String userId = getIntent().getStringExtra("userId");
                if (userId == null) {
                    com.google.firebase.auth.FirebaseAuth auth = com.google.firebase.auth.FirebaseAuth.getInstance();
                    com.google.firebase.auth.FirebaseUser currentUser = auth.getCurrentUser();
                    if (currentUser != null) {
                        userId = currentUser.getUid();
                    }
                }
                if (userId != null) {
                    historyIntent.putExtra("userId", userId);
                }
                startActivity(historyIntent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
                Intent profileIntent = new Intent(BloodInventoryActivity.this, Profile.class);
                // Pass userId if available
                String userId = getIntent().getStringExtra("userId");
                if (userId == null) {
                    com.google.firebase.auth.FirebaseAuth auth = com.google.firebase.auth.FirebaseAuth.getInstance();
                    com.google.firebase.auth.FirebaseUser currentUser = auth.getCurrentUser();
                    if (currentUser != null) {
                        userId = currentUser.getUid();
                    }
                }
                if (userId != null) {
                    profileIntent.putExtra("userId", userId);
                }
                startActivity(profileIntent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double theta = lon1 - lon2;
        double dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2))
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.cos(Math.toRadians(theta));
        dist = Math.acos(dist);
        dist = Math.toDegrees(dist);
        dist = dist * 60 * 1.1515;
        return dist * 1.609344;
    }

    @Override
    public void onBackPressed() {
        // Navigate back to Dashboard
        Intent intent = new Intent(BloodInventoryActivity.this, Dashboard.class);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
        finish();
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
                return true;
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
}
